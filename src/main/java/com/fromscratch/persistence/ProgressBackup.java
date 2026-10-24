package com.fromscratch.persistence;

import com.fromscratch.bank.BankRun;
import com.google.gson.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;
import java.util.zip.*;
import net.runelite.client.util.Filepath;

/** Portable, bounded, checksummed local exports. Worker only; all filesystem access uses Filepath.
 * A restore stages and verifies a complete tree, then atomically switches one local directory pointer.
 * Old trees are retained verbatim. Economic and playtime formats are unchanged.
 */
public final class ProgressBackup
{
	private static final int MAX_FILES=100000,MAX_FILE=4*1024*1024,MAX_TOTAL=256*1024*1024;
	private final BankRepository repository;
	private final BankRunCodec codec;
	ProgressBackup(BankRepository repository,BankRunCodec codec) { this.repository=repository;this.codec=codec; }
	static Filepath directory(Filepath base,BankRunCodec codec) throws IOException
	{
		Filepath pointer=base.joinSegment("restored-directory.json");if(!pointer.exists()){return base;}
		try
		{
			JsonObject p=codec.payload(BankStorageFiles.readText(pointer),1);BankRunCodec.keys(p,"directory");String name=BankRunCodec.str(p,"directory");
			if(!name.matches("restored-[a-f0-9-]{36}")){throw new IllegalArgumentException();}
			Filepath selected=base.joinSegment(name);if(!selected.isDirectory()){throw new IllegalArgumentException();}return selected;
		}
		catch(RuntimeException ex){throw new SaveException("Restored progress location is invalid. Existing files were preserved.");}
	}
	public void exportProgress(Filepath root,BankRun expected,Filepath target) throws IOException
	{
		if(target.startsWith(root)){throw new SaveException("Choose an export location outside the plugin's progression directory.");}
		if(target.exists()){throw new SaveException("That backup file already exists. Choose a new filename to keep the previous backup safe.");}
		locked(root,expected.getCharacter(),expected.getRunId(),base ->
		{
			Filepath selected=directory(base,codec);BankRun current=codec.decode(BankStorageFiles.readText(selected.joinSegment("current.json")),expected.getCharacter());
			if(!current.getRevision().equals(expected.getRevision())){throw new SaveException("Progress changed. Try exporting again.");}
			repository.verify(selected,current,true);
			writeExport(selected,current,target);return null;
		});
	}
	private void writeExport(Filepath selected,BankRun run,Filepath target) throws IOException
	{
		validatePlaytimeDirectory(selected,run);
		List<Filepath> files;try(Stream<Filepath> walk=selected.walk(2)) { files=walk.filter(Filepath::isFile).filter(f->allowed(relative(selected,f))).sorted().collect(Collectors.toList()); }
		if(files.size()>MAX_FILES){throw new SaveException("Backup contains too many files.");}
		// Chooser selections are rooted AT the selected file: accessing their parent is forbidden.
		// Build privately first, then create a new explicitly selected file. Never overwrite an older backup.
		Filepath pending=selected.createTempFile("fromscratch-export-",".tmp");JsonObject hashes=new JsonObject();long total=0;
		try
		{
		try(ZipOutputStream zip=new ZipOutputStream(pending.openOutputStream()))
		{
			for(Filepath file:files)
			{
				byte[] bytes=read(file.openInputStream(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS),MAX_FILE);total+=bytes.length;if(total>MAX_TOTAL){throw new SaveException("Backup exceeds 256 MB.");}
				String path=relative(selected,file);hashes.addProperty(path,sha(bytes));zip.putNextEntry(new ZipEntry(path));zip.write(bytes);zip.closeEntry();
			}
			JsonObject manifest=new JsonObject();manifest.addProperty("character",run.getCharacter());manifest.addProperty("run",run.getRunId().toString());manifest.addProperty("revision",run.getRevision().toString());manifest.addProperty("schema",BankRunCodec.FORMAT);manifest.addProperty("exportedAt",Instant.now().toString());manifest.add("files",hashes);
			byte[] manifestBytes=codec.envelope(manifest,1).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			if(manifestBytes.length>MAX_FILE||total+manifestBytes.length>MAX_TOTAL){throw new SaveException("Backup manifest exceeds the portable size limit.");}
			zip.putNextEntry(new ZipEntry("manifest.json"));zip.write(manifestBytes);zip.closeEntry();
		}
		force(pending);boolean created=false;
		try
		{
			try(OutputStream output=target.openOutputStream(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE))
			{created=true;try(InputStream input=pending.openInputStream()){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){output.write(buffer,0,n);}}}
			force(target);
		}
		catch(IOException ex){if(created){target.deleteIfExists();}if(ex instanceof FileAlreadyExistsException){throw new SaveException("That backup file already exists. Choose a new filename to keep the previous backup safe.");}throw ex;}
		}
		finally{pending.deleteIfExists();}
	}
	/** Extract into a private, unselected tree. No current progression is changed by preview/cancel. */
	public Candidate preview(Filepath root,String character,Filepath source) throws IOException
	{
		if(character==null||!character.matches("[a-f0-9]{64}")){throw new SaveException("Log in to select a character before importing.");}
		Filepath staging=root.createTempDir("progress-import-");Filepath dest=staging.join("characters",character);dest.createDirectories();
		try
		{
			Map<String,String> hashes=new TreeMap<>();Set<String> occupied=new HashSet<>();JsonObject manifest=null;long total=0;int count=0;
			try(ZipInputStream zip=new ZipInputStream(source.openInputStream()))
			{
				ZipEntry entry;while((entry=zip.getNextEntry())!=null)
				{
					String name=entry.getName();if(entry.isDirectory()||++count>MAX_FILES+1||(!name.equals("manifest.json")&&!allowed(name))||!occupied.add(name.toLowerCase(Locale.ROOT))){throw new SaveException("Backup contains an invalid or duplicate entry.");}
					byte[] bytes=readEntry(zip,MAX_FILE);total+=bytes.length;if(total>MAX_TOTAL){throw new SaveException("Backup exceeds 256 MB.");}
					if(name.equals("manifest.json")){if(manifest!=null){throw new SaveException("Duplicate backup manifest.");}manifest=codec.payload(new String(bytes,java.nio.charset.StandardCharsets.UTF_8),1);}
					else{Filepath file=dest.join(name);file.getParent().createDirectories();file.write(bytes,StandardOpenOption.CREATE_NEW);force(file);hashes.put(name,sha(bytes));}
					zip.closeEntry();
				}
			}
			if(manifest==null){throw new SaveException("Backup manifest is missing.");}
			BankRunCodec.keys(manifest,"character","run","revision","schema","exportedAt","files");
			long schema=BankRunCodec.number(manifest.get("schema"));if(!character.equals(BankRunCodec.str(manifest,"character"))||schema<9||schema>BankRunCodec.FORMAT){throw new SaveException("Backup belongs to another character or uses an incompatible schema.");}
			JsonObject declared=manifest.getAsJsonObject("files");if(declared.size()!=hashes.size()||!hashes.containsKey("current.json")){throw new SaveException("Backup is incomplete.");}
			for(Map.Entry<String,String> entry:hashes.entrySet()){if(!declared.has(entry.getKey())||!entry.getValue().equals(declared.get(entry.getKey()).getAsString())){throw new SaveException("Backup checksum failed.");}}
			BankRun run=repository.load(staging,character).orElseThrow(()->new SaveException("Backup has no run."));
			if(!run.isActive()||!run.getRunId().toString().equals(BankRunCodec.str(manifest,"run"))||!run.getRevision().toString().equals(BankRunCodec.str(manifest,"revision"))){throw new SaveException("Backup run does not match its manifest.");}
			validatePlaytime(staging,run);
			String fingerprint=locked(root,character,run.getRunId(),base->fingerprint(base));
			return new Candidate(staging,run,fingerprint);
		}
		catch(IOException|RuntimeException ex){staging.deleteRecursively();if(ex instanceof IOException){throw (IOException)ex;}throw new SaveException("Backup is malformed or incompatible. Current progression was not changed.");}
	}
	public BankRun restore(Filepath root,Candidate candidate) throws IOException
	{
		return locked(root,candidate.run.getCharacter(),candidate.run.getRunId(),true,base ->
		{
			if(!candidate.fingerprint.equals(fingerprint(base))){throw new SaveException("Local progress changed. Import again and review the newer state before replacing it.");}
			BankRun verified=repository.load(candidate.staging,candidate.run.getCharacter()).orElseThrow(()->new SaveException("Staged backup is incomplete."));
			if(!verified.equals(candidate.run)){throw new SaveException("Staged backup changed.");}validatePlaytime(candidate.staging,verified);
			Filepath previous=directory(base,codec);
			// Keep an explicit portable backup before switching. Corrupt current bytes also remain in the untouched tree.
			if(previous.joinSegment("current.json").exists())
			{
				BankRun old=null;try{old=codec.decode(BankStorageFiles.readText(previous.joinSegment("current.json")),verified.getCharacter());}catch(SaveException|RuntimeException ex){/* Preserve damaged evidence below. */}
				if(old!=null){writeExport(previous,old,base.joinSegment("before-import-"+UUID.randomUUID()+".fsp"));}
				else
				{
					// Preserve damaged evidence exactly; it is never presented as a valid importable run.
					Filepath damaged=base.joinSegment("before-import-damaged-"+UUID.randomUUID());damaged.createDirectory();
					Filepath original=previous.joinSegment("current.json"),copy=damaged.joinSegment("current.json");original.copyTo(copy);force(copy);
					if(!BankStorageFiles.readText(original).equals(BankStorageFiles.readText(copy))){throw new SaveException("Damaged-save backup failed. Nothing was replaced.");}
				}
			}
			String name="restored-"+UUID.randomUUID();Filepath selected=base.joinSegment(name);
			candidate.staging.join("characters",verified.getCharacter()).moveTo(selected,StandardCopyOption.ATOMIC_MOVE);
			JsonObject p=new JsonObject();p.addProperty("directory",name);Filepath pending=base.joinSegment("restore-pointer-"+UUID.randomUUID()+".json");BankStorageFiles.writeNew(pending,codec.envelope(p,1));
			pending.moveTo(base.joinSegment("restored-directory.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
			return verified;
		});
	}
	public void discard(Candidate candidate) throws IOException { candidate.staging.deleteRecursively(); }
	/** Explicit recovery only: never automatically select an unpublished checkpoint or discard a valid head. */
	public Candidate previous(Filepath root,String character) throws IOException
	{
		try{if(repository.load(root,character).isPresent()){throw new IllegalArgumentException("Current progress is valid. Retry loading it instead.");}}
		catch(SaveException damaged){/* Preserve the damaged head, offer only the previously published bytes. */}
		Filepath base=root.join("characters",character),selected=directory(base,codec),file=selected.joinSegment("previous.json");
		if(!file.exists()){throw new SaveException("No verified previous save is available. Import a compatible exported backup instead.");}
		BankRun run=codec.decode(BankStorageFiles.readText(file),character);if(!run.isActive()){throw new SaveException("The previous checkpoint has no active run. Import a compatible exported backup instead.");}Filepath staging=root.createTempDir("progress-import-");Filepath dest=staging.join("characters",character);dest.createDirectories();
		try
		{
			try(Stream<Filepath> walk=selected.walk(2))
			{
				for(Filepath source:walk.filter(Filepath::isFile).filter(f->allowed(relative(selected,f))&&!relative(selected,f).equals("current.json")).collect(Collectors.toList()))
				{Filepath copy=dest.join(relative(selected,source));copy.getParent().createDirectories();source.copyTo(copy);force(copy);}
			}
			BankStorageFiles.writeNew(dest.joinSegment("current.json"),BankStorageFiles.readText(file));
			BankRun verified=repository.load(staging,character).orElseThrow(()->new SaveException("Previous save is incomplete."));validatePlaytime(staging,verified);
			return new Candidate(staging,run,locked(root,character,run.getRunId(),d->fingerprint(d)));
		}
		catch(IOException|RuntimeException ex){staging.deleteRecursively();throw ex;}
	}
	private void validatePlaytime(Filepath root,BankRun run) throws IOException
	{validatePlaytimeDirectory(root.join("characters",run.getCharacter()),run);}
	private void validatePlaytimeDirectory(Filepath selected,BankRun run) throws IOException
	{
		new PeakProgressRepository(codec).read(selected,run);
		Filepath directory=selected.joinSegment("playtime"),file=directory.joinSegment("playtime-"+run.getRunId()+".json");
		if(file.exists()){new PlaytimeRepository(codec).read(file,run.getCharacter(),run.getRunId());}
		else if(directory.isDirectory())
		{
			try(Stream<Filepath> files=directory.walk(1)){if(files.anyMatch(f->f.getFileName().startsWith("playtime-"+run.getRunId()+"-")&&f.getFileName().endsWith(".json"))){throw new SaveException("Playtime checkpoint is missing. Preserve its recovery files before exporting or importing.");}}
		}
	}
	private String fingerprint(Filepath base) throws IOException
	{
		Filepath selected=directory(base,codec),current=selected.joinSegment("current.json"),pointer=base.joinSegment("restored-directory.json");
		String peaks="";try(Stream<Filepath> files=selected.walk(1)){for(Filepath file:files.filter(f->f.getFileName().matches("peak-[a-f0-9-]{36}\\.json")).sorted().collect(Collectors.toList())){peaks+=sha(read(file.openInputStream(),MAX_FILE));}}
		return SaveIntegrity.sha256((pointer.exists()?sha(read(pointer.openInputStream(),MAX_FILE)):"")+"\n"+(current.exists()?sha(read(current.openInputStream(),MAX_FILE)):"")+"\n"+peaks);
	}
	private static boolean allowed(String name)
	{
		return name.matches("(?:current|previous|previous-pending-[a-f0-9-]{36}|bank-(?:ledger|checkpoint|pending)-[a-f0-9-]{36}|(?:checkpoint|ledger|archive|migration-v[0-9]+)-[a-zA-Z0-9-]+)\\.json")
			||name.matches("bank-migration-[a-f0-9-]{36}/[a-zA-Z0-9-]+\\.json")
			||name.matches("peak-[a-f0-9-]+\\.json")
			||name.matches("playtime/playtime-[a-f0-9-]+\\.json");
	}
	private static String relative(Filepath root,Filepath path) { return path.toString().substring(root.toString().length()+1).replace('\\','/'); }
	private static byte[] read(InputStream input,int max) throws IOException { try(InputStream close=input){return readEntry(close,max);} }
	private static byte[] readEntry(InputStream input,int max) throws IOException
	{
		ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=input.read(buf))!=-1){if(out.size()>max-n){throw new SaveException("Backup entry is too large.");}out.write(buf,0,n);}return out.toByteArray();
	}
	private static String sha(byte[] bytes)
	{
		try{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)){out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));}return out.toString();}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
	}
	private static void force(Filepath path) throws IOException { try(FileChannel channel=path.openFileChannel(StandardOpenOption.WRITE)){channel.force(true);} }
	private <T>T locked(Filepath root,String character,UUID run,Work<T> work) throws IOException
	{return locked(root,character,run,false,work);}
	private <T>T locked(Filepath root,String character,UUID run,boolean restoring,Work<T> work) throws IOException
	{
		if(character==null||!character.matches("[a-f0-9]{64}")){throw new SaveException("Unverified character.");}
		Filepath base=root.join("characters",character);base.createDirectories();base.joinSegment("playtime").createDirectories();
		try(FileChannel bank=base.joinSegment("run.lock").openFileChannel(StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock bankLock=bank.tryLock())
		{
			if(bankLock==null){throw new SaveException("Progress is being saved. Try again shortly.");}
			// Export reads atomically replaced time files without holding up the independent minute checkpoint.
			if(!restoring){return work.run(base);}
			try(FileChannel time=base.join("playtime","playtime.lock").openFileChannel(StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock timeLock=time.tryLock())
			{if(timeLock==null){throw new SaveException("Time played is being saved. Try again shortly.");}return work.run(base);}
		}
		catch(OverlappingFileLockException ex){throw new SaveException("Progress is being saved. Try again shortly.");}
	}
	@FunctionalInterface private interface Work<T>{T run(Filepath base)throws IOException;}
	public static final class Candidate
	{
		private final Filepath staging;private final BankRun run;private final String fingerprint;
		Candidate(Filepath staging,BankRun run,String fingerprint){this.staging=staging;this.run=run;this.fingerprint=fingerprint;}
		public BankRun getRun(){return run;}
	}
}
