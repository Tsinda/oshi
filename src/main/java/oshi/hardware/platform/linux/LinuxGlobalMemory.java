/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.FileUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class LinuxGlobalMemory extends AbstractGlobalMemory {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(LinuxGlobalMemory.class);

	// Values read from /proc/meminfo used for other calculations
	private long memFree = 0;
	private long activeFile = 0;
	private long inactiveFile = 0;
	private long sReclaimable = 0;
	private long swapFree = 0;

	private long lastUpdate = 0;

	/**
	 * Updates instance variables from reading /proc/meminfo no more frequently
	 * than every 100ms. While most of the information is available in the
	 * sysinfo structure, the most accurate calculation of MemAvailable is only
	 * available from reading this pseudo-file. The maintainers of the Linux
	 * Kernel have indicated this location will be kept up to date if the
	 * calculation changes: see
	 * https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?
	 * id=34e431b0ae398fc54ea69ff85ec700722c9da773
	 * 
	 * Internally, reading /proc/meminfo is faster than sysinfo because it only
	 * spends time populating the memory components of the sysinfo structure.
	 */
	protected void updateMeminfo() {
		long now = System.currentTimeMillis();
		if (now - this.lastUpdate > 100) {
			List<String> memInfo = null;
			memInfo = FileUtil.readFile("/proc/meminfo");
			if (memInfo.isEmpty()) {
				return;
			}
			boolean found = false;
			for (String checkLine : memInfo) {
				String[] memorySplit = checkLine.split("\\s+");
				if (memorySplit.length > 1) {
					if (memorySplit[0].equals("MemTotal:")) {
						this.memTotal = parseMeminfo(memorySplit);
						break;
					} else if (memorySplit[0].equals("MemFree:")) {
						this.memFree = parseMeminfo(memorySplit);
						break;
					} else if (memorySplit[0].equals("MemAvailable:")) {
						this.memAvailable = parseMeminfo(memorySplit);
						found = true;
						break;
					} else if (memorySplit[0].equals("Active(file):")) {
						this.activeFile = parseMeminfo(memorySplit);
						break;
					} else if (memorySplit[0].equals("Inactive(file):")) {
						this.inactiveFile = parseMeminfo(memorySplit);
						break;
					} else if (memorySplit[0].equals("SReclaimable:")) {
						this.sReclaimable = parseMeminfo(memorySplit);
						break;
					} else if (memorySplit[0].equals("SwapTotal:")) {
						this.swapTotal = parseMeminfo(memorySplit);
						break;
					} else if (memorySplit[0].equals("SwapFree:")) {
						this.swapFree = parseMeminfo(memorySplit);
						break;
					} else
						break;
				}
			}
			this.swapUsed = this.swapTotal - this.swapFree;
			// If no MemAvailable, calculate from other fields
			if (!found) {
				this.memAvailable = this.memFree + this.activeFile + this.inactiveFile + this.sReclaimable;
			}

			this.lastUpdate = now;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateSwap() {
		updateMeminfo();
	}

	/**
	 * Parses lines from the display of /proc/meminfo
	 * 
	 * @param memorySplit
	 *            Array of Strings representing the 3 columns of /proc/meminfo
	 * @return value, multiplied by 1024 if kB is specified
	 */
	private long parseMeminfo(String[] memorySplit) {
		if (memorySplit.length < 2) {
			return 0l;
		}
		long memory = 0L;
		try {
			memory = Long.parseLong(memorySplit[1]);
		} catch (NumberFormatException nfe) {
			LOG.error("Unable to parse " + memorySplit[1] + " to a long integer.");
			return 0L;
		}
		if (memorySplit.length > 2 && memorySplit[2].equals("kB")) {
			memory *= 1024;
		}
		return memory;
	}
}
