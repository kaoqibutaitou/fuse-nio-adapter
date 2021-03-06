package org.cryptomator.frontend.fuse;

import java.nio.file.AccessMode;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import jnr.posix.util.Platform;
import ru.serce.jnrfuse.flags.AccessConstants;
import ru.serce.jnrfuse.struct.FileStat;

@PerAdapter
public class FileAttributesUtil {

	// uid/gid are overwritten by fuse mount options -ouid=...
	private static final int DUMMY_UID = 65534; // usually nobody
	private static final int DUMMY_GID = 65534; // usually nobody

	@Inject
	public FileAttributesUtil() {
	}

	public Set<AccessMode> accessModeMaskToSet(int mask) {
		Set<AccessMode> accessModes = EnumSet.noneOf(AccessMode.class);
		// @formatter:off
		if ((mask & AccessConstants.R_OK) == AccessConstants.R_OK) accessModes.add(AccessMode.READ);
		if ((mask & AccessConstants.W_OK) == AccessConstants.W_OK) accessModes.add(AccessMode.WRITE);
		if ((mask & AccessConstants.X_OK) == AccessConstants.X_OK) accessModes.add(AccessMode.EXECUTE);
		// @formatter:on
		return accessModes;
	}

	public Set<PosixFilePermission> octalModeToPosixPermissions(long mode) {
		Set<PosixFilePermission> result = EnumSet.noneOf(PosixFilePermission.class);
		// @formatter:off
		if ((mode & 0400) == 0400) result.add(PosixFilePermission.OWNER_READ);
		if ((mode & 0200) == 0200) result.add(PosixFilePermission.OWNER_WRITE);
		if ((mode & 0100) == 0100) result.add(PosixFilePermission.OWNER_EXECUTE);
		if ((mode & 0040) == 0040) result.add(PosixFilePermission.GROUP_READ);
		if ((mode & 0020) == 0020) result.add(PosixFilePermission.GROUP_WRITE);
		if ((mode & 0010) == 0010) result.add(PosixFilePermission.GROUP_EXECUTE);
		if ((mode & 0004) == 0004) result.add(PosixFilePermission.OTHERS_READ);
		if ((mode & 0002) == 0002) result.add(PosixFilePermission.OTHERS_WRITE);
		if ((mode & 0001) == 0001) result.add(PosixFilePermission.OTHERS_EXECUTE);
		// @formatter:on
		return result;
	}

	public FileStat basicFileAttributesToFileStat(BasicFileAttributes attrs) {
		FileStat stat = new FileStat(jnr.ffi.Runtime.getSystemRuntime());
		copyBasicFileAttributesFromNioToFuse(attrs, stat);
		return stat;
	}

	public void copyBasicFileAttributesFromNioToFuse(BasicFileAttributes attrs, FileStat stat) {
		if (attrs.isDirectory()) {
			stat.st_mode.set(stat.st_mode.longValue() | FileStat.S_IFDIR);
		} else {
			stat.st_mode.set(stat.st_mode.longValue() | FileStat.S_IFREG);
		}
		stat.st_uid.set(DUMMY_UID);
		stat.st_gid.set(DUMMY_GID);
		stat.st_mtim.tv_sec.set(attrs.lastModifiedTime().toInstant().getEpochSecond());
		stat.st_mtim.tv_nsec.set(attrs.lastModifiedTime().toInstant().getNano());
		stat.st_ctim.tv_sec.set(attrs.creationTime().toInstant().getEpochSecond());
		stat.st_ctim.tv_nsec.set(attrs.creationTime().toInstant().getNano());
		if (Platform.IS_MAC || Platform.IS_WINDOWS) {
			assert stat.st_birthtime != null;
			stat.st_birthtime.tv_sec.set(attrs.creationTime().toInstant().getEpochSecond());
			stat.st_birthtime.tv_nsec.set(attrs.creationTime().toInstant().getNano());
		}
		stat.st_atim.tv_sec.set(attrs.lastAccessTime().toInstant().getEpochSecond());
		stat.st_atim.tv_nsec.set(attrs.lastAccessTime().toInstant().getNano());
		stat.st_size.set(attrs.size());
	}

}
