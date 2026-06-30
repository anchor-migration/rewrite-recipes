package com.anchor.migration.rewrite.cli;

import com.anchor.migration.rewrite.cmp.CmpForeignKeyToJpa;
import com.anchor.migration.rewrite.cmp.CmpManyToManyToJpa;
import com.anchor.migration.rewrite.cmp.CmpScalarEntityToJpa;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apply a single OpenRewrite recipe to one {@code .java} file on disk (ADR-007 §3.3 E2E helper).
 *
 * <p>Usage: {@code ApplyRecipeMain <recipe-name> <path-to-java-file> [targetClassName]}
 */
public final class ApplyRecipeMain {

    private static final List<String> RECIPE_NAMES =
            List.of("CmpScalarEntityToJpa", "CmpManyToManyToJpa", "CmpForeignKeyToJpa");

    private ApplyRecipeMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: ApplyRecipeMain <recipe-name> <path-to-java-file> [targetClassName]");
            System.err.println("Recipes: " + RECIPE_NAMES);
            System.err.println("Optional targetClassName applies to CmpScalarEntityToJpa (e.g. TxBean, CustomerBean).");
            System.exit(1);
        }

        String targetClassName = args.length == 3 ? args[2] : null;
        Recipe recipe = resolveRecipe(args[0], targetClassName);
        if (recipe == null) {
            System.err.println("Unknown recipe: " + args[0]);
            System.exit(1);
        }

        Path file = Path.of(args[1]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            System.err.println("Not a file: " + file);
            System.exit(1);
        }

        JavaParser parser = JavaParser.fromJavaVersion().build();
        String source = Files.readString(file);
        List<SourceFile> inputs = new ArrayList<>();
        parser.parse(source).forEach(parsed -> inputs.add(parsed.withSourcePath(file)));

        InMemoryLargeSourceSet sourceSet = new InMemoryLargeSourceSet(inputs);
        ExecutionContext ctx =
                new InMemoryExecutionContext(
                        throwable -> {
                            throw new RuntimeException(throwable);
                        });
        RecipeRun run = recipe.run(sourceSet, ctx);

        boolean changed = false;
        for (Result result : run.getChangeset().getAllResults()) {
            SourceFile after = result.getAfter();
            if (after == null) {
                continue;
            }
            Path resultPath = after.getSourcePath();
            if (resultPath != null && !file.equals(resultPath.toAbsolutePath().normalize())) {
                continue;
            }
            Files.writeString(file, after.printAll());
            changed = true;
            System.out.println("Updated: " + file);
        }

        if (!changed) {
            System.out.println("No changes: " + file);
        }
    }

    static Recipe resolveRecipe(String name, String targetClassName) {
        return switch (name) {
            case "CmpScalarEntityToJpa" -> {
                CmpScalarEntityToJpa recipe = new CmpScalarEntityToJpa();
                if (targetClassName != null && !targetClassName.isBlank()) {
                    recipe.targeting(targetClassName);
                }
                yield recipe;
            }
            case "CmpManyToManyToJpa" -> new CmpManyToManyToJpa();
            case "CmpForeignKeyToJpa" -> new CmpForeignKeyToJpa();
            default -> null;
        };
    }
}
