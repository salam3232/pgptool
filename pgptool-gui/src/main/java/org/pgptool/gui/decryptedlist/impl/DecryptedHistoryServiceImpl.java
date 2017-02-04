/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.pgptool.gui.decryptedlist.impl;

import java.io.File;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.List;

import org.apache.log4j.Logger;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.decryptedlist.api.DecryptedHistoryService;
import org.pgptool.gui.tools.fileswatcher.FilesWatcherHandler;
import org.pgptool.gui.tools.fileswatcher.MultipleFilesWatcher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class DecryptedHistoryServiceImpl implements DecryptedHistoryService, InitializingBean, DisposableBean {
	private static Logger log = Logger.getLogger(DecryptedHistoryServiceImpl.class);

	private static final String PREFIX = "decrhist:";

	@Autowired
	private ConfigPairs configPairs;
	@Autowired
	private EventBus eventBus;

	private MultipleFilesWatcher multipleFilesWatcher;

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO: Fix. Smells like DI violation
		multipleFilesWatcher = new MultipleFilesWatcher(dirWatcherHandler, "DecryptedHistoryService");
		List<DecryptedFile> entries = getDecryptedFiles();
		for (DecryptedFile entry : entries) {
			String decryptedFile = entry.getDecryptedFile();
			if (!new File(decryptedFile).exists()) {
				log.debug("Previously registered file is no longer exists, remove =ing it from the tracking "
						+ decryptedFile);
				remove(decryptedFile);
				continue;
			}
			multipleFilesWatcher.watchForFileChanges(decryptedFile);
		}
	}

	@Override
	public void destroy() throws Exception {
		multipleFilesWatcher.stopWatcher();
	}

	private FilesWatcherHandler dirWatcherHandler = new FilesWatcherHandler() {
		@Override
		public void handleFileChanged(Kind<?> entryDelete, String fileAbsolutePathname) {
			if (!StandardWatchEventKinds.ENTRY_DELETE.equals(entryDelete)) {
				return;
			}
			remove(fileAbsolutePathname);
		}
	};

	@Override
	public void add(DecryptedFile decryptedFile) {
		try {
			Preconditions.checkArgument(decryptedFile != null, "decryptedFile is NULL");
			Preconditions.checkArgument(decryptedFile.getDecryptedFile() != null,
					"decryptedFile.DecryptedFile is NULL");
			Preconditions.checkArgument(decryptedFile.getEncryptedFile() != null,
					"decryptedFile.EncryptedFile is NULL");

			configPairs.put(buildKey(decryptedFile.getDecryptedFile()), decryptedFile);
			eventBus.post(EntityChangedEvent.added(decryptedFile));
			multipleFilesWatcher.watchForFileChanges(decryptedFile.getDecryptedFile());
		} catch (Throwable t) {
			throw new RuntimeException("Failed to register decrypted file for monitoring", t);
		}
	}

	@Override
	public void remove(String depcryptedFilePathname) {
		try {
			Preconditions.checkArgument(depcryptedFilePathname != null, "depcryptedFilePathname is NULL");

			String key = buildKey(depcryptedFilePathname);
			DecryptedFile existing = configPairs.find(key, null);
			if (existing == null) {
				// if it's not there -- nothing to delete
				return;
			}

			configPairs.put(key, null);
			eventBus.post(EntityChangedEvent.removedObject(existing));
			multipleFilesWatcher.stopWatchingFile(depcryptedFilePathname);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to unregister decrypted file from monitoring", t);
		}
	}

	private String buildKey(String decryptedFile) {
		return PREFIX + decryptedFile;
	}

	@Override
	public List<DecryptedFile> getDecryptedFiles() {
		return configPairs.findAllWithPrefixedKey(PREFIX);
	}

}
