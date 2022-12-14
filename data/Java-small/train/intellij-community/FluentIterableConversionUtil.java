/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.typeMigration.rules.guava;

import com.intellij.codeInsight.daemon.impl.analysis.JavaGenericsUtil;
import com.intellij.codeInspection.AnonymousCanBeLambdaInspection;
import com.intellij.codeInspection.java18StreamApi.StreamApiConstants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.typeMigration.TypeConversionDescriptor;
import com.intellij.refactoring.typeMigration.TypeEvaluator;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.text.UniqueNameGenerator;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import com.siyeh.ipp.types.ReplaceMethodRefWithLambdaIntention;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class FluentIterableConversionUtil {
  private final static Logger LOG = Logger.getInstance(FluentIterableConversionUtil.class);

  @Nullable
  static TypeConversionDescriptor getToArrayDescriptor(PsiType initialType, PsiExpression expression) {
    if (!(initialType instanceof PsiClassType)) {
      return null;
    }
    final PsiType[] parameters = ((PsiClassType)initialType).getParameters();
    if (parameters.length != 1) {
      return null;
    }
    final PsiElement methodCall = expression.getParent();
    if (!(methodCall instanceof PsiMethodCallExpression)) {
      return null;
    }

    final PsiExpression[] expressions = ((PsiMethodCallExpression)methodCall).getArgumentList().getExpressions();
    if (expressions.length != 1) {
      return null;
    }
    final PsiExpression classTypeExpression = expressions[0];
    final PsiType targetType = classTypeExpression.getType();
    if (!(targetType instanceof PsiClassType)) {
      return null;
    }
    final PsiType[] targetParameters = ((PsiClassType)targetType).getParameters();
    if (targetParameters.length != 1) {
      return null;
    }
    if (PsiTypesUtil.compareTypes(parameters[0], targetParameters[0], false)) {
      return new TypeConversionDescriptor("$q$.toArray($type$)", null) {
        PsiType myType = parameters[0];

        @Override
        public PsiExpression replace(PsiExpression expression, TypeEvaluator evaluator) throws IncorrectOperationException {
          if (!JavaGenericsUtil.isReifiableType(myType)) {
            final String chosenName = chooseName(expression, PsiType.INT);
            final PsiType arrayType;
            if (myType instanceof PsiClassType) {
              final PsiClass resolvedClass = ((PsiClassType)myType).resolve();
              if (resolvedClass == null) return expression;
              if (resolvedClass instanceof PsiTypeParameter) {
                arrayType = PsiType.getJavaLangObject(expression.getManager(), expression.getResolveScope());
              } else {
                arrayType = JavaPsiFacade.getElementFactory(expression.getProject()).createType(resolvedClass);
              }
            }  else {
              return null;
            }
            setReplaceByString("$q$.toArray(" + chosenName + " -> " + "(" + myType.getCanonicalText(false) + "[]) new " +
                               arrayType.getCanonicalText(false) + "[" + chosenName + "])");
          } else {
            setReplaceByString("$q$.toArray(" + myType.getCanonicalText(false) + "[]::new)");
          }
          return super.replace(expression, evaluator);
        }
      };
    }
    return null;
  }

  public static String chooseName(@NotNull PsiExpression context, @Nullable PsiType type) {
    final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
    final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(context.getProject());
    final String name = codeStyleManager.suggestUniqueVariableName(
      codeStyleManager.suggestVariableName(VariableKind.LOCAL_VARIABLE, null, null, type).names[0], context, false);
    return nameGenerator.generateUniqueName(name);
  }

  @Nullable
  static TypeConversionDescriptor getFilterDescriptor(PsiMethod method) {
    LOG.assertTrue("filter".equals(method.getName()));

    final PsiParameter[] parameters = method.getParameterList().getParameters();
    if (parameters.length != 1) return null;
    final PsiParameter parameter = parameters[0];
    final PsiType type = parameter.getType();
    if (!(type instanceof PsiClassType)) return null;
    final PsiClass resolvedClass = ((PsiClassType)type).resolve();
    if (resolvedClass == null) return null;
    if (CommonClassNames.JAVA_LANG_CLASS.equals(resolvedClass.getQualifiedName())) {
      return new GuavaFilterInstanceOfConversionDescriptor();
    }
    else if (GuavaPredicateConversionRule.GUAVA_PREDICATE.equals(resolvedClass.getQualifiedName())) {
      return new GuavaTypeConversionDescriptor("$it$.filter($p$)", "$it$." + StreamApiConstants.FILTER + "($p$)");
    }
    return null;
  }

  static class TransformAndConcatConversionRule extends GuavaTypeConversionDescriptor {
    public TransformAndConcatConversionRule() {
      super("$q$.transformAndConcat($params$)", "$q$.flatMap($params$)");
    }

    @Override
    public PsiExpression replace(PsiExpression expression, TypeEvaluator typeEvaluator) {
      PsiExpression argument = ((PsiMethodCallExpression)expression).getArgumentList().getExpressions()[0];

      PsiAnonymousClass anonymousClass;
      if (argument instanceof PsiNewExpression &&
          (anonymousClass = ((PsiNewExpression)argument).getAnonymousClass()) != null) {
        if (AnonymousCanBeLambdaInspection.canBeConvertedToLambda(anonymousClass, true)) {
          argument = AnonymousCanBeLambdaInspection.replacePsiElementWithLambda(argument, true, true);
        };
      }
      final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(expression.getProject());
      if (argument != null && !(argument instanceof PsiFunctionalExpression)) {
        argument =
          (PsiExpression)argument.replace(javaPsiFacade.getElementFactory().createExpressionFromText("(" + argument.getText() + ")::apply", null));
        ParenthesesUtils.removeParentheses(argument, false);
      }

      if (argument instanceof PsiMethodReferenceExpression) {
        argument = ReplaceMethodRefWithLambdaIntention.convertMethodReferenceToLambda((PsiMethodReferenceExpression)argument, true);
      }
      if (argument instanceof PsiLambdaExpression) {
        List<Pair<PsiExpression, Boolean>> iterableReturnValues = new SmartList<Pair<PsiExpression, Boolean>>();

        final PsiElement body = ((PsiLambdaExpression)argument).getBody();
        final PsiClass collection = javaPsiFacade.findClass(CommonClassNames.JAVA_UTIL_COLLECTION, expression.getResolveScope());
        if (collection == null) return expression;
        final PsiClass iterable = javaPsiFacade.findClass(CommonClassNames.JAVA_LANG_ITERABLE, expression.getResolveScope());
        if (iterable == null) return expression;

        if (body instanceof PsiCodeBlock) {
          for (PsiReturnStatement statement : PsiUtil.findReturnStatements((PsiCodeBlock)body)) {
            final PsiExpression retValue = statement.getReturnValue();
            if (!determineType(retValue, iterableReturnValues, iterable, collection)) {
              return expression;
            }
          }
        } else if (!(body instanceof PsiExpression) || !determineType((PsiExpression)body, iterableReturnValues, iterable, collection)) {
          return expression;
        }

        for (Pair<PsiExpression, Boolean> returnValueAndIsCollection : iterableReturnValues) {
          convertToStream(returnValueAndIsCollection.getFirst(), returnValueAndIsCollection.getSecond());
        }

      } else {
        return expression;
      }

      return super.replace(expression, typeEvaluator);
    }

    private static boolean determineType(PsiExpression retValue,
                                         List<Pair<PsiExpression, Boolean>> iterableReturnValues,
                                         PsiClass iterable,
                                         PsiClass collection) {
      if (retValue == null) return false;
      PsiType type = retValue.getType();
      if (PsiType.NULL.equals(type)) {
        return true;
      }
      if (type instanceof PsiCapturedWildcardType) {
        type = ((PsiCapturedWildcardType)type).getUpperBound();
      }
      if (type instanceof PsiClassType) {
        final PsiClass resolvedClass = ((PsiClassType)type).resolve();

        if (InheritanceUtil.isInheritorOrSelf(resolvedClass, iterable, true)) {
          final boolean isCollection = InheritanceUtil.isInheritorOrSelf(resolvedClass, collection, true);
          iterableReturnValues.add(Pair.create(retValue, isCollection));
          return true;
        }
      }
      return false;
    }

    private static void convertToStream(@NotNull PsiExpression returnValue, boolean isCollection) {
      final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(returnValue.getProject());
      PsiExpression newExpression;
      if (isCollection) {
        String expressionAsText = "(" + returnValue.getText() + ").stream()";
        newExpression = elementFactory.createExpressionFromText(expressionAsText, returnValue);
        ParenthesesUtils.removeParentheses(newExpression, false);
      }
      else {
        final String methodCall = "(" + returnValue.getText() + ")";
        final boolean needParentheses = ParenthesesUtils
          .areParenthesesNeeded((PsiParenthesizedExpression)elementFactory.createExpressionFromText(methodCall, null), false);
        String expressionAsText = "java.util.stream.StreamSupport.stream(" + (needParentheses ? methodCall : methodCall.substring(1, methodCall.length() - 1)) + ".spliterator(), false)";
        newExpression = elementFactory.createExpressionFromText(expressionAsText, returnValue);
      }
      returnValue.replace(newExpression);
    }
  }

  private static class GuavaFilterInstanceOfConversionDescriptor extends TypeConversionDescriptor {
    public GuavaFilterInstanceOfConversionDescriptor() {
      super("$it$.filter($p$)", "$it$." + StreamApiConstants.FILTER + "($p$)");
    }

    @Override
    public PsiExpression replace(PsiExpression expression, TypeEvaluator evaluator) {
      final PsiExpression argument = ((PsiMethodCallExpression)expression).getArgumentList().getExpressions()[0];
      final PsiExpression newArgument = JavaPsiFacade.getElementFactory(expression.getProject()).createExpressionFromText("(" + argument.getText() + ")::isInstance", argument);
      ParenthesesUtils.removeParentheses((PsiExpression)((PsiMethodReferenceExpression)newArgument).getQualifier(), false);
      argument.replace(newArgument);
      return super.replace(expression, evaluator);
    }
  }

  static TypeConversionDescriptor createToCollectionDescriptor(@Nullable String methodName,
                                                               @NotNull PsiExpression context) {
    final String findTemplate;
    final String replaceTemplate;
    final String returnType;
    if ("toMap".equals(methodName) || "uniqueIndex".equals(methodName)) {
      final GuavaTypeConversionDescriptor descriptor = new GuavaTypeConversionDescriptor("$it$.$methodName$($f$)",
                                                                                         "$it$.collect(java.util.stream.Collectors.toMap(java.util.function.Function.identity(), $f$))");
      return descriptor.withConversionType(GuavaConversionUtil.addTypeParameters(CommonClassNames.JAVA_UTIL_MAP, context.getType(), context));
    }
    else if ("toList".equals(methodName)) {
      findTemplate = "$it$.toList()";
      replaceTemplate = GuavaFluentIterableConversionRule.STREAM_COLLECT_TO_LIST;
      returnType = CommonClassNames.JAVA_UTIL_LIST;
    }
    else if ("toSet".equals(methodName)) {
      findTemplate = "$it$.toSet()";
      replaceTemplate = "$it$.collect(java.util.stream.Collectors.toSet())";
      returnType = CommonClassNames.JAVA_UTIL_SET;
    }
    else if ("toSortedList".equals(methodName)) {
      findTemplate = "$it$.toSortedList($c$)";
      replaceTemplate = "$it$.sorted($c$).collect(java.util.stream.Collectors.toList())";
      returnType = CommonClassNames.JAVA_UTIL_LIST;
    }
    else if ("toSortedSet".equals(methodName)) {
      findTemplate = "$it$.toSortedSet($c$)";
      replaceTemplate = "$it$.collect(java.util.stream.Collectors.toCollection(() -> new java.util.TreeSet<>($c$)))";
      returnType = CommonClassNames.JAVA_UTIL_SET;
    } else {
      return null;
    }
    final PsiType type = GuavaConversionUtil.addTypeParameters(returnType, context.getType(), context);
    return new TypeConversionDescriptor(findTemplate, replaceTemplate).withConversionType(type);
  }
}
