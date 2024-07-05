package io.papermc.generator.types.registry;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.papermc.generator.Main;
import io.papermc.generator.types.SimpleGenerator;
import io.papermc.generator.utils.Annotations;
import io.papermc.generator.utils.Formatting;
import io.papermc.generator.utils.Javadocs;
import io.papermc.generator.utils.RegistryUtils;
import io.papermc.generator.utils.experimental.SingleFlagHolder;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static io.papermc.generator.utils.Annotations.EXPERIMENTAL_API_ANNOTATION;
import static io.papermc.generator.utils.Annotations.NOT_NULL;
import static io.papermc.generator.utils.Annotations.experimentalAnnotations;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class GeneratedTagKeyType<T, A> extends SimpleGenerator {

    private final Class<A> apiType;
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final RegistryKey<A> apiRegistryKey;
    private final boolean publicCreateKeyMethod;

    public GeneratedTagKeyType(final String className, final Class<A> apiType, final String packageName, final ResourceKey<? extends Registry<T>> registryKey, final RegistryKey<A> apiRegistryKey, final boolean publicCreateKeyMethod) {
        super(className, packageName);
        this.apiType = apiType;
        this.registryKey = registryKey;
        this.apiRegistryKey = apiRegistryKey;
        this.publicCreateKeyMethod = publicCreateKeyMethod;
    }

    private MethodSpec.Builder createMethod(final TypeName returnType) {
        final TypeName keyType = TypeName.get(Key.class).annotated(NOT_NULL);

        final ParameterSpec keyParam = ParameterSpec.builder(keyType, "key", FINAL).build();
        final MethodSpec.Builder create = MethodSpec.methodBuilder("create")
            .addModifiers(this.publicCreateKeyMethod ? PUBLIC : PRIVATE, STATIC)
            .addParameter(keyParam)
            .addCode("return $T.create($T.$L, $N);", TagKey.class, RegistryKey.class, requireNonNull(RegistryUtils.REGISTRY_KEY_FIELD_NAMES.get(this.apiRegistryKey)), keyParam)
            .returns(returnType.annotated(NOT_NULL));
        if (this.publicCreateKeyMethod) {
            create.addAnnotation(EXPERIMENTAL_API_ANNOTATION); // TODO remove once not experimental
            create.addJavadoc(Javadocs.CREATE_TYPED_KEY_JAVADOC, this.apiType, this.registryKey.location().toString());
        }
        return create;
    }

    private TypeSpec.Builder keyHolderType() {
        return classBuilder(this.className)
            .addModifiers(PUBLIC, FINAL)
            .addJavadoc(Javadocs.getVersionDependentClassHeader("{@link $T#$L}"), RegistryKey.class, requireNonNull(RegistryUtils.REGISTRY_KEY_FIELD_NAMES.get(this.apiRegistryKey)))
            .addAnnotations(Annotations.CLASS_HEADER)
            .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .build()
            );
    }

    @Override
    protected TypeSpec getTypeSpec() {
        final TypeName tagKeyType = ParameterizedTypeName.get(TagKey.class, this.apiType);

        final TypeSpec.Builder typeBuilder = this.keyHolderType();
        final MethodSpec.Builder createMethod = this.createMethod(tagKeyType);

        final Registry<T> registry = Main.REGISTRY_ACCESS.registryOrThrow(this.registryKey);

        final AtomicBoolean allExperimental = new AtomicBoolean(true);
        registry.getTagNames().sorted(Formatting.alphabeticKeyOrder(tagKey -> tagKey.location().getPath())).forEach(tagKey -> {
            final String fieldName = Formatting.formatKeyAsField(tagKey.location().getPath());
            final FieldSpec.Builder fieldBuilder = FieldSpec.builder(tagKeyType, fieldName, PUBLIC, STATIC, FINAL)
                .initializer("$N(key($S))", createMethod.build(), tagKey.location().getPath())
                .addJavadoc(Javadocs.getVersionDependentField("{@code $L}"), "#" + tagKey.location());

            final String featureFlagName = Main.EXPERIMENTAL_TAGS.get(tagKey);
            if (featureFlagName != null) {
                fieldBuilder.addAnnotations(experimentalAnnotations(SingleFlagHolder.fromVanillaName(featureFlagName)));
            } else {
                allExperimental.set(false);
            }
            typeBuilder.addField(fieldBuilder.build());
        });
        if (allExperimental.get()) {
            typeBuilder.addAnnotation(EXPERIMENTAL_API_ANNOTATION);
            createMethod.addAnnotation(EXPERIMENTAL_API_ANNOTATION);
        } else {
            typeBuilder.addAnnotation(EXPERIMENTAL_API_ANNOTATION); // TODO experimental API
        }
        return typeBuilder.addMethod(createMethod.build()).build();
    }

    @Override
    protected JavaFile.Builder file(final JavaFile.Builder builder) {
        return builder.addStaticImport(Key.class, "key");
    }
}
