package com.romainpiel.androidpermissionsusage;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.Key;
import com.intellij.psi.SdkResolveScopeProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;

/**
 * Created by rpiel on 21/06/2014.
 */
public class AndroidSDKScope {

    private static final Key<GlobalSearchScope> ANDROID_SDK_SCOPE_KEY = new Key<GlobalSearchScope>("ANDROID_SDK_SCOPE_KEY");
    private static final Key<GlobalSearchScope> ALL_EXCEPT_ANDROID_SDK_SCOPE_KEY = new Key<GlobalSearchScope>("ALL_EXCEPT_ANDROID_SDK_SCOPE_KEY");

    public static GlobalSearchScope getScope(Module module) {
        Project project = module.getProject();
        GlobalSearchScope cached = project.getUserData(ANDROID_SDK_SCOPE_KEY);
        if (cached != null) {
            return cached;
        } else {
            init(module);
            return project.getUserData(ANDROID_SDK_SCOPE_KEY);
        }
    }

    public static GlobalSearchScope getAllExceptScope(Module module) {
        Project project = module.getProject();
        GlobalSearchScope cached = project.getUserData(ALL_EXCEPT_ANDROID_SDK_SCOPE_KEY);
        if (cached != null) {
            return cached;
        } else {
            init(module);
            return project.getUserData(ALL_EXCEPT_ANDROID_SDK_SCOPE_KEY);
        }
    }

    private static void init(final Module module) {
        OrderEnumerator.orderEntries(module).sdkOnly().forEach(new Processor<OrderEntry>() {
            @Override
            public boolean process(OrderEntry orderEntry) {

                Project project = module.getProject();

                JdkOrderEntry jdkOrderEntry = (JdkOrderEntry) orderEntry;
                for (SdkResolveScopeProvider provider : SdkResolveScopeProvider.EP_NAME.getExtensions()) {
                    final GlobalSearchScope sdkScope = provider.getScope(project, jdkOrderEntry);
                    if (sdkScope != null) {

                        project.putUserData(ANDROID_SDK_SCOPE_KEY, sdkScope);

                        GlobalSearchScope allScopeExceptSdk = GlobalSearchScope.allScope(project).intersectWith(GlobalSearchScope.notScope(sdkScope));
                        project.putUserData(ALL_EXCEPT_ANDROID_SDK_SCOPE_KEY, allScopeExceptSdk);

                        break;
                    }
                }
                return false;
            }
        });
    }
}
