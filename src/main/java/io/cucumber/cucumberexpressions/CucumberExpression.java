package io.cucumber.cucumberexpressions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CucumberExpression implements Expression {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^\\}:]+)(:([^\\}]+))?\\}");
    private static final Pattern OPTIONAL_PATTERN = Pattern.compile("\\(([^\\)]+)\\)");

    private final Pattern pattern;
    private final List<Transform<?>> transforms = new ArrayList<>();
    private final String expression;

    public CucumberExpression(final String expression, List<Class> targetTypes, TransformLookup transformLookup) {
        this.expression = expression;
        String expressionWithOptionalGroups = OPTIONAL_PATTERN.matcher(expression).replaceAll("(?:$1)?");
        Matcher matcher = VARIABLE_PATTERN.matcher(expressionWithOptionalGroups);

        StringBuffer sb = new StringBuffer();
        sb.append("^");
        int typeNameIndex = 0;
        while (matcher.find()) {
            Class targetType = targetTypes.size() <= typeNameIndex ? null : targetTypes.get(typeNameIndex++);
            String expressionTypeName = matcher.group(3);

            Transform transform;
            if (expressionTypeName != null) {
                transform = transformLookup.lookup(expressionTypeName);
            } else if (targetType != null) {
                transform = transformLookup.lookup(targetType);
            } else {
                transform = transformLookup.lookup(String.class);
            }
            transforms.add(transform);

            matcher.appendReplacement(sb, Matcher.quoteReplacement("(" + transform.getCaptureGroupRegexps().get(0) + ")"));
        }
        matcher.appendTail(sb);
        sb.append("$");

        pattern = Pattern.compile(sb.toString());
    }

    @Override
    public List<Argument> match(String text) {
        return ArgumentMatcher.matchArguments(pattern, text, transforms);
    }

    @Override
    public String getSource() {
        return expression;
    }

    Pattern getPattern() {
        return pattern;
    }
}
