package com.look4counter.modeleditor

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

object ActivityClassNavigation {
    fun open(project: Project, qualifiedName: String): Boolean {
        val name = qualifiedName.trim()
        if (name.isEmpty() || project.isDisposed || DumbService.isDumb(project)) return false
        val type = ReadAction.compute<PsiClass?, RuntimeException> {
            JavaPsiFacade.getInstance(project).findClass(name, GlobalSearchScope.allScope(project))
        } ?: return false
        val target = type.navigationElement as? Navigatable ?: return false
        if (!target.canNavigate()) return false
        target.navigate(true)
        return true
    }
}
