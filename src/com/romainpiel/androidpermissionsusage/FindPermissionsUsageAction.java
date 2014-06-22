package com.romainpiel.androidpermissionsusage;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;

public class FindPermissionsUsageAction extends AnAction {

    protected FindPermissionsUsageAction() {
        super("Find Permissions Usage...", "Find Permissions Usage", null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        final VirtualFile file = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (file == null) {
            return;
        }

        final Project project = event.getProject();
        if (project == null) {
            return;
        }

        final Module module = ModuleUtil.findModuleForFile(file, project);

        JavaPsiFacade psi = JavaPsiFacade.getInstance(project);
        final PsiClass manifestClass = psi.findClass("android.Manifest.permission", AndroidSDKScope.getScope(module));

        ProgressManager.getInstance().run(new FindPermissionUsagesTask(file, module, manifestClass));
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null || files.length == 0 || !files[0].getName().equals("AndroidManifest.xml")) {
            e.getPresentation().setVisible(false);
        } else {
            e.getPresentation().setVisible(true);
        }
    }
}