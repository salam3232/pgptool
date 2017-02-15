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
package org.pgptool.gui.ui.tempfolderfordecrypted;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.FolderChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.validation.FieldValidationException;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

/**
 * This simple presenter will provide user with ability to select other folder
 * to store temporarily decrypted files
 * 
 * @author Sergey Karpushin
 *
 */
public class TempFolderChooserPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(TempFolderChooserPm.class);

	@Autowired
	private DecryptedTempFolder decryptedTempFolder;

	private FolderChooserDialog folderChooserDialog;

	public void present(Component parent) {
		try {
			String newFolder = getFolderChooserDialog().askUserForFolder(parent);
			if (newFolder == null) {
				return;
			}
			UiUtils.messageBox(parent, text("phrase.settingsChangedConfirmFolder", newFolder), text("term.success"),
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Throwable t) {
			log.error("Failed to save settigns for folder to use for temp decrypted files", t);
			EntryPoint.reportExceptionToUser("error.failedToSetNewTempFolder", t);
		}
	}

	public FolderChooserDialog getFolderChooserDialog() {
		if (folderChooserDialog == null) {
			ValueAdapter<String> recentlyUsedFolder = new ValueAdapter<String>() {
				@Override
				public void setValue(String value) {
					try {
						decryptedTempFolder.setTempFolderBasePath(value);
					} catch (FieldValidationException e) {
						throw new RuntimeException("Failed to persist temp folder location", e);
					}
				}

				@Override
				public String getValue() {
					return decryptedTempFolder.getTempFolderBasePath();
				}
			};
			folderChooserDialog = new FolderChooserDialog(text("term.changeTempFolderForDecrypted"),
					recentlyUsedFolder);
		}
		return folderChooserDialog;
	}
}
