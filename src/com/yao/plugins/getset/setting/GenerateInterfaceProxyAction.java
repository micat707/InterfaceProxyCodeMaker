package com.yao.plugins.getset.setting;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.CollectionListModel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by adfasdf on 2017/8/31.
 */
public class GenerateInterfaceProxyAction extends AnAction {

    public GenerateInterfaceProxyAction() {
    }

    public void actionPerformed(AnActionEvent e) {
        this.generateInterfaceProxyMethod(this.getPsiMethodFromContext(e));
    }

    private void generateInterfaceProxyMethod(final PsiClass psiMethod) {
        (new WriteCommandAction.Simple(psiMethod.getProject(), new PsiFile[]{psiMethod.getContainingFile()}) {
            protected void run() throws Throwable {
                GenerateInterfaceProxyAction.this.createInterfaceProxy(psiMethod);
            }
        }).execute();
    }

    private void createInterfaceProxy(PsiClass psiClass) {
        List fields = (new CollectionListModel(psiClass.getFields())).getItems();
        if(fields != null) {
            List list = (new CollectionListModel(psiClass.getMethods())).getItems();
            List fieldlist = (new CollectionListModel(psiClass.getFields())).getItems();
            HashSet methodSet = new HashSet();
            HashSet fieldSet = new HashSet();
            Iterator elementFactory = list.iterator();
            Iterator feildelementFactory = list.iterator();
            while(elementFactory.hasNext()) {
                PsiMethod m = (PsiMethod)elementFactory.next();
                methodSet.add(m.getName());
            }
            while(feildelementFactory.hasNext()) {
                PsiField f = (PsiField)elementFactory.next();
                fieldSet.add(f.getName());
            }
            PsiElementFactory elementFactory1 = JavaPsiFacade.getElementFactory(psiClass.getProject());

            PsiClass[] interfacesclasses = psiClass.getInterfaces();
            if(interfacesclasses != null  && interfacesclasses.length > 0){
                for(PsiClass currentPsiClass : interfacesclasses){
                    String interfaceType  = currentPsiClass.getName();
                    PsiField field = elementFactory1.createFieldFromText(String.format("private %s %s;",
                            interfaceType, generatorInterfaceDelegate(interfaceType))
                            , psiClass);
                    if(!fieldSet.contains(field.getName())){
                        psiClass.add(field);
                    }
                    processOneInterface(currentPsiClass, psiClass, methodSet, elementFactory1);
                }
            }


        }
    }
    private String generatorInterfaceDelegate(String interfaceName){
     return "delegate" + interfaceName;
    }
    private void processOneInterface(PsiClass currentPsiClass, PsiClass parrentPsiClass, HashSet methodSet, PsiElementFactory elementFactory) {
        PsiMethod[] psiMethods = currentPsiClass.getAllMethods();
        String supperClassWithoutObject = currentPsiClass.getSupers().toString().replace("", "");
        if(psiMethods != null && psiMethods.length > 0){
            for (PsiMethod currentPsemethod : psiMethods){
                if(! currentPsemethod.getContainingClass().isInterface()){//如果该方法所处的类不是接口，则不处理
                    continue;
                }
                String delegateName = generatorInterfaceDelegate( currentPsiClass.getName());
                processOneMethod(currentPsemethod, parrentPsiClass, methodSet, elementFactory, delegateName);
            }
        }
    }
    private void processOneMethod(PsiMethod currentPsemethod, PsiClass parrentPsiClass , HashSet methodSet,
                                  PsiElementFactory elementFactory, String delegateName){
        String methodText = converPsiMethodToText(currentPsemethod, delegateName);
        PsiMethod newMethod = elementFactory.createMethodFromText(methodText, parrentPsiClass);
        if(!methodSet.contains(currentPsemethod.getName())) {
            parrentPsiClass.add(newMethod);
            //methodSet.add(currentPsemethod.getName());
        }

    }
    private String converPsiMethodToText(PsiMethod currentPsemethod, String deleteName){
        StringBuilder sb = new StringBuilder();
        PsiParameter[] parameters = currentPsemethod.getParameterList().getParameters();
        String returnType = currentPsemethod.getReturnType().getPresentableText();
        String newReturnType = "";
        if(currentPsemethod.getText().contains("<")){//有泛型
            newReturnType = String.format("<%s> %s", returnType, returnType);
            returnType = newReturnType;
        }
        String methodName = currentPsemethod.getName();
        //sb.append(" @Override\r\n");
        String paramBody = "()";
        StringBuilder paramBodySb = new StringBuilder();
        StringBuilder paramWithNoTypeBodySb = new StringBuilder();
        if(parameters != null && parameters.length > 0){
            paramBodySb.append("(");
            paramWithNoTypeBodySb.append("(");
            for(int i = 0; i < parameters.length; i ++){
                if(i != 0){
                    paramBodySb.append(", ");
                    paramWithNoTypeBodySb.append(", ");
                }
                paramBodySb.append(parameters[i].getType().getPresentableText());
                paramBodySb.append(" ");
                paramBodySb.append(parameters[i].getName());
                paramWithNoTypeBodySb.append(parameters[i].getName());
            }
            paramBodySb.append(")");
            paramWithNoTypeBodySb.append(")");
            paramBody = paramBodySb.toString();
        }else{
            paramBody = "()";
            paramWithNoTypeBodySb.append("()");
        }
        String throwsBody = currentPsemethod.getThrowsList().getText();


        sb.append(String.format("public %s %s%s %s {", returnType,methodName, paramBody, throwsBody ));
        String returnStr = "";
        if(! returnType.equals("void")){
            returnStr = "return";
        }
        String delegate = deleteName;
        String methodBody = String.format("%s %s.%s%s;", returnStr, delegate, methodName, paramWithNoTypeBodySb.toString());
        sb.append(methodBody);
        sb.append("    }");
        return sb.toString();
    }





    private PsiClass getPsiMethodFromContext(AnActionEvent e) {
        PsiElement elementAt = this.getPsiElement(e);
        return elementAt == null?null:(PsiClass) PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }

    private PsiElement getPsiElement(AnActionEvent e) {
        PsiFile psiFile = (PsiFile)e.getData(LangDataKeys.PSI_FILE);
        Editor editor = (Editor)e.getData(PlatformDataKeys.EDITOR);
        if(psiFile != null && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            return psiFile.findElementAt(offset);
        } else {
            e.getPresentation().setEnabled(false);
            return null;
        }
    }
}

