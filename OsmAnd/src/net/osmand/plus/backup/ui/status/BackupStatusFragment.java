package net.osmand.plus.backup.ui.status;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupError;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.AuthorizeFragment.LoginDialogType;
import net.osmand.plus.backup.ui.BackupAndRestoreFragment;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;
import java.util.Map;

public class BackupStatusFragment extends BaseOsmAndFragment implements BackupExportListener,
		OnDeleteFilesListener, OnPrepareBackupListener, ImportListener, InAppPurchaseListener {

	private OsmandApplication app;
	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private BackupStatusAdapter adapter;
	private ProgressBar progressBar;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_status, container, false);

		progressBar = view.findViewById(R.id.progress_bar);

		adapter = new BackupStatusAdapter(getMapActivity(), this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateAdapter();
		settingsHelper.updateExportListener(this);
		settingsHelper.updateImportListener(this);
		backupHelper.addPrepareBackupListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		settingsHelper.updateExportListener(null);
		settingsHelper.updateImportListener(null);
		backupHelper.removePrepareBackupListener(this);
	}

	@Nullable
	public LoginDialogType getDialogType() {
		Fragment parent = getParentFragment();
		if (parent instanceof BackupAndRestoreFragment) {
			return ((BackupAndRestoreFragment) parent).getDialogType();
		}
		return null;
	}

	public void removeDialogType() {
		Fragment parent = getParentFragment();
		if (parent instanceof BackupAndRestoreFragment) {
			((BackupAndRestoreFragment) parent).removeDialogType();
		}
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.setBackup(backupHelper.getBackup());
		}
	}

	@Override
	public void onBackupPreparing() {
		AndroidUiHelper.setVisibility(View.VISIBLE, progressBar);
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		AndroidUiHelper.setVisibility(View.INVISIBLE, progressBar);
		updateAdapter();
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@Override
	public void onBackupExportStarted() {
		if (adapter != null) {
			adapter.onBackupExportStarted();
		}
	}

	@Override
	public void onBackupExportProgressUpdate(int value) {
		if (adapter != null) {
			adapter.onBackupExportProgressUpdate(value);
		}
	}

	@Override
	public void onBackupExportFinished(@Nullable String error) {
		if (error != null) {
			updateAdapter();
			app.showShortToastMessage(new BackupError(error).getLocalizedError(app));
		} else if (!settingsHelper.isBackupExporting()) {
			backupHelper.prepareBackup();
		}
	}

	@Override
	public void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		if (adapter != null) {
			adapter.onBackupExportItemStarted(type, fileName, work);
		}
	}

	@Override
	public void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName) {
		if (adapter != null) {
			adapter.onBackupExportItemFinished(type, fileName);
		}
	}

	@Override
	public void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		if (adapter != null) {
			adapter.onBackupExportItemProgress(type, fileName, value);
		}
	}

	@Override
	public void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {

	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
		if (adapter != null) {
			adapter.onFileDeleteProgress(file, progress);
		}
	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
		if (adapter != null) {
			adapter.onFilesDeleteDone(errors);
		}
	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {
		if (adapter != null) {
			adapter.onFilesDeleteError(status, message);
		}
	}

	@Override
	public void onImportItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		if (adapter != null) {
			adapter.onImportItemStarted(type, fileName, work);
		}
	}

	@Override
	public void onImportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		if (adapter != null) {
			adapter.onImportItemProgress(type, fileName, value);
		}
	}

	@Override
	public void onImportItemFinished(@NonNull String type, @NonNull String fileName) {
		if (adapter != null) {
			adapter.onImportItemFinished(type, fileName);
		}
	}

	@Override
	public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
		if (!settingsHelper.isBackupExporting() && !settingsHelper.isBackupImporting()) {
			backupHelper.prepareBackup();
		} else {
			adapter.onImportFinished(succeed, needRestart, items);
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		backupHelper.prepareBackup();
	}
}