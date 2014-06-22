package com.romainpiel.androidpermissionsusage;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.javadoc.PsiDocMethodOrFieldRef;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rpiel on 22/06/2014.
 */
public class FindPermissionUsagesTask extends Task.Backgroundable {

    private static final Condition<PsiElement> CLASS_OR_METHOD_OR_FIELD_CONDITION = new Condition<PsiElement>() {
        @Override
        public boolean value(PsiElement psiElement) {
            return psiElement instanceof PsiClass || psiElement instanceof PsiMethod || psiElement instanceof PsiField;
        }
    };

    private VirtualFile file;
    private Module module;
    private PsiClass manifestClass;
    private HashMap<String, List<PsiElement>> results;

    public FindPermissionUsagesTask(VirtualFile file, final Module module, final PsiClass manifestClass) {
        super(module.getProject(), "Searching for permissions usage...");
        this.file = file;
        this.module = module;
        this.manifestClass = manifestClass;
        this.results = new HashMap<String, List<PsiElement>>();
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            ManifestPermissionsParser.findPermissions(file.getInputStream(), new ManifestPermissionsParser.OnPermissionFoundListener() {
                @Override
                public void onPermissionFound(String path) {
                    try {
                        String name = ManifestPermissionsParser.getPermissionName(path);
                        findPermissionUsages(module, manifestClass, name);
                    } catch (IllegalStateException ignored) {}
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findPermissionUsages(Module module, PsiClass manifestClass, String permissionName) throws IllegalStateException{
        PsiField field = manifestClass.findFieldByName(permissionName, false);
        if (field == null) {
            throw new IllegalStateException("Android permission doesn't exist");
        }

        List<PsiElement> resultValue = new ArrayList<PsiElement>();
        results.put(permissionName, resultValue);

        Collection<PsiReference> references = ReferencesSearch.search(field, AndroidSDKScope.getScope(module)).findAll();
        for (PsiReference reference : references) {
            // we only need nodes with this kind {@link blah}
            if (reference instanceof PsiDocMethodOrFieldRef.MyReference) {
                PsiElement firstParent = PsiTreeUtil.findFirstParent(reference.getElement(), CLASS_OR_METHOD_OR_FIELD_CONDITION);

                if (firstParent != null) {
                    resultValue.add(firstParent);
                }
            }
        }
    }

    @Override
    public void onSuccess() {

        String newLine = System.getProperty("line.separator");

        StringBuilder stringBuilder = new StringBuilder();

        for (String permissionName : results.keySet()) {
            stringBuilder.append("--- ")
                    .append(permissionName)
                    .append(" ---");
            List<PsiElement> resultValue = results.get(permissionName);
            for (PsiElement element : resultValue) {
                int refCount = findReferenceUsage(module, element);
                if (refCount > 0) {

                    String name = "";

                    if (element instanceof PsiClass) {
                        PsiClass clazz = ((PsiClass) element);
                        name = clazz.getQualifiedName();
                    } else if (element instanceof PsiMethod) {
                        PsiMethod method = ((PsiMethod) element);
                        name = method.getContainingClass().getQualifiedName() + "." + method.getName();
                    } else if (element instanceof PsiField) {
                        PsiField variable = ((PsiField) element);
                        name = variable.getContainingClass().getQualifiedName() + "." + variable.getName();
                    }
                    stringBuilder.append(newLine)
                            .append(name)
                            .append(" ")
                            .append(refCount);
                }
            }

            stringBuilder.append(newLine).append(newLine);
        }

        Messages.showInfoMessage(module.getProject(), stringBuilder.toString(), "Permissions Usage");
    }

    private int findReferenceUsage(Module module, PsiElement element) {
        Collection<PsiReference> references = ReferencesSearch.search(element, AndroidSDKScope.getAllExceptScope(module)).findAll();
        return references.size();
    }
}
