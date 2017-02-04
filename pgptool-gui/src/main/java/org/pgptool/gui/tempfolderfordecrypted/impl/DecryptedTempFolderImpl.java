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
package org.pgptool.gui.tempfolderfordecrypted.impl;

import java.io.File;
import java.util.Random;

import org.apache.log4j.Logger;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.tools.TextFile;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import org.summerb.approaches.i18n.I18nUtils;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationError;
import org.summerb.approaches.validation.errors.FieldRequiredValidationError;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class DecryptedTempFolderImpl implements DecryptedTempFolder, InitializingBean, ApplicationContextAware {
	public static final String CONFIG_DECRYPTED_TEMP_FOLDER = "tempFolderForDecrypted";
	private static Logger log = Logger.getLogger(DecryptedTempFolderImpl.class);

	@Autowired
	private ConfigsBasePathResolver configsBasePathResolver;
	@Autowired
	private ConfigPairs configPairs;

	private String tempFolderBasePath;
	private ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() throws Exception {
		tempFolderBasePath = configPairs.find(CONFIG_DECRYPTED_TEMP_FOLDER,
				configsBasePathResolver.getConfigsBasePath() + File.separator + "decrypted");
		ensureDirIsCreated(tempFolderBasePath);
	}

	private void ensureDirIsCreated(String dir) {
		File file = new File(dir);
		Preconditions.checkState(file.exists() || file.mkdirs(),
				"Failed to ensure all dirs created during path: " + dir);
	}

	@Override
	public String getTempFolderBasePath() {
		return tempFolderBasePath;
	}

	@Override
	public void setTempFolderBasePath(String newValue) throws FieldValidationException {
		validate(newValue);
		tempFolderBasePath = newValue;
		configPairs.put(CONFIG_DECRYPTED_TEMP_FOLDER, tempFolderBasePath);
	}

	@SuppressWarnings("deprecation")
	private void validate(String newValue) throws FieldValidationException {
		try {
			if (!StringUtils.hasText(newValue)) {
				throw new FieldValidationException(new FieldRequiredValidationError(CONFIG_DECRYPTED_TEMP_FOLDER));
			}
			ensureDirIsCreated(newValue);
			ensureWeCanCreateFilesThere(newValue);
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, FieldValidationException.class);
			log.error("Exception during validation of target folder for temp decrypted files " + newValue, t);
			throw new FieldValidationException(new ValidationError("error.temporaryFolderCannotbeUsed",
					CONFIG_DECRYPTED_TEMP_FOLDER, I18nUtils.buildMessagesChain(t, applicationContext)));
		}
	}

	private void ensureWeCanCreateFilesThere(String basePath) throws Exception {
		String testFile = basePath + new Random().nextInt(Integer.MAX_VALUE);
		try {
			TextFile.write(testFile, "test");
			new File(testFile).delete();
		} catch (Throwable t) {
			throw new Exception("Failed to verify if temporary folder can actually be used", t);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
