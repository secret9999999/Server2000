package io.papermc.generator.types.registry;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.papermc.generator.Main;
import io.papermc.generator.types.SimpleGenerator;
import io.papermc.generator.utils.Annotations;
import io.papermc.generator.utils.Formatting;
import io.papermc.generator.utils.Javadocs;
import io.papermc.generator.utils.TagRegistry;
import io.papermc.generator.utils.experimental.SingleFlagHolder;
import java.util.Locale;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static io.papermc.generator.utils.Annotations.experimentalAnnotations;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class TagGenerator extends SimpleGenerator {

    private static final String REGISTRY_FIELD_JAVADOC = "Key for the built-in $L registry.";

    public TagGenerator(final String className, final String packageName) {
        super(className, packageName);
    }

    private TypeSpec.Builder tagHolderType() {
        return interfaceBuilder(this.className)
            .addModifiers(PUBLIC)
            .addSuperinterface(Keyed.class)
            .addAnnotations(Annotations.CLASS_HEADER);
    }

    @Override
    protected TypeSpec getTypeSpec() {
        final TypeVariableName typeVariable = TypeVariableName.get("T", Keyed.class);
        final TypeSpec.Builder typeBuilder = this.tagHolderType();
        typeBuilder.addTypeVariable(typeVariable);

        for (final TagRegistry tagRegistry : TagRegistry.SUPPORTED_REGISTRIES) {
            final TypeName fieldType = ParameterizedTypeName.get(Tag.class, tagRegistry.apiType());

            final ResourceKey<? extends Registry<?>> registryKey = tagRegistry.registryKey();
            final Registry<?> registry = Main.REGISTRY_ACCESS.registryOrThrow(registryKey);

            final String registryFieldName = "REGISTRY_" + tagRegistry.folderName().toUpperCase(Locale.ENGLISH);
            final FieldSpec.Builder registryFieldBuilder = FieldSpec.builder(String.class, registryFieldName)
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer("$S", tagRegistry.folderName())
                .addJavadoc(REGISTRY_FIELD_JAVADOC, registryKey.location().getPath());

            typeBuilder.addField(registryFieldBuilder.build());

            final String fieldPrefix = Formatting.formatTagFieldPrefix(tagRegistry.folderName(), registryKey);

            registry.getTagNames().sorted(Formatting.alphabeticKeyOrder(tagKey -> tagKey.location().getPath())).forEach(tagKey -> {
                final String keyPath = tagKey.location().getPath();

                final String fieldName = fieldPrefix + Formatting.formatKeyAsField(keyPath);
                final FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName)
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .initializer("$T.getTag($L, $T.minecraft($S), $T.class)", Bukkit.class, registryFieldName, NamespacedKey.class, keyPath, tagRegistry.apiType())
                    .addJavadoc(Javadocs.getVersionDependentField("{@code $L}"), tagKey.location().toString());
                String featureFlagName = Main.EXPERIMENTAL_TAGS.get(tagKey);
                if (featureFlagName != null) {
                    fieldBuilder.addAnnotations(experimentalAnnotations(SingleFlagHolder.fromVanillaName(featureFlagName)));
                }
                typeBuilder.addField(fieldBuilder.build());
            });
        }

        return typeBuilder.build();
    }
}
