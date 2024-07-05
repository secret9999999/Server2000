package io.papermc.generator.types.registry;

import com.google.common.base.Suppliers;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import io.papermc.generator.Main;
import io.papermc.generator.types.SimpleGenerator;
import io.papermc.generator.utils.Annotations;
import io.papermc.generator.utils.Formatting;
import io.papermc.generator.utils.RegistryUtils;
import io.papermc.generator.utils.experimental.FlagHolders;
import io.papermc.generator.utils.experimental.SingleFlagHolder;
import java.util.Set;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlags;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.checkerframework.checker.nullness.qual.Nullable;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

public abstract class EnumRegistryGenerator<T> extends SimpleGenerator {

    private final Registry<T> registry;
    private final Supplier<Set<ResourceKey<T>>> experimentalKeys;
    private final boolean isFilteredRegistry;

    public EnumRegistryGenerator(final String className, final String packageName, final ResourceKey<? extends Registry<T>> registryKey) {
        super(className, packageName);
        this.registry = Main.REGISTRY_ACCESS.registryOrThrow(registryKey);
        this.experimentalKeys = Suppliers.memoize(() -> RegistryUtils.collectExperimentalDataDrivenKeys(this.registry));
        this.isFilteredRegistry = FeatureElement.FILTERED_REGISTRIES.contains(registryKey);
    }

    @Override
    protected TypeSpec getTypeSpec() {
        TypeSpec.Builder typeBuilder = TypeSpec.enumBuilder(this.className)
            .addSuperinterface(Keyed.class)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotations(Annotations.CLASS_HEADER);

        this.registry.holders().sorted(Formatting.alphabeticKeyOrder(reference -> reference.key().location().getPath())).forEach(reference -> {
            ResourceKey<T> resourceKey = reference.key();
            String pathKey = resourceKey.location().getPath();

            String fieldName = Formatting.formatKeyAsField(pathKey);
            @Nullable SingleFlagHolder requiredFeature = this.getRequiredFeature(reference);
            TypeSpec.Builder builder = TypeSpec.anonymousClassBuilder("$S", pathKey);
            if (requiredFeature != null) {
                builder.addAnnotations(Annotations.experimentalAnnotations(requiredFeature));
            }

            typeBuilder.addEnumConstant(fieldName, builder.build());
        });

        FieldSpec keyField = FieldSpec.builder(NamespacedKey.class, "key", PRIVATE, FINAL).build();
        typeBuilder.addField(keyField);

        ParameterSpec keyParam = ParameterSpec.builder(String.class, "key").build();
        typeBuilder.addMethod(MethodSpec.constructorBuilder()
            .addParameter(keyParam).addCode("this.$N = $T.minecraft($N);", keyField, NamespacedKey.class, keyParam).build());

        typeBuilder.addMethod(MethodSpec.methodBuilder("getKey")
            .returns(NamespacedKey.class)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Annotations.NOT_NULL)
            .addAnnotation(Annotations.OVERRIDE)
            .addCode("return this.$N;", keyField).build());

        this.addExtras(typeBuilder, keyField);

        return typeBuilder.build();
    }

    public abstract void addExtras(TypeSpec.Builder builder, FieldSpec keyField);

    public @Nullable SingleFlagHolder getRequiredFeature(Holder.Reference<T> reference) {
        if (this.isFilteredRegistry) {
            // built-in registry
            FeatureElement element = (FeatureElement) reference.value();
            if (FeatureFlags.isExperimental(element.requiredFeatures())) {
                return SingleFlagHolder.fromSet(element.requiredFeatures());
            }
        } else {
            // data-driven registry
            if (this.experimentalKeys.get().contains(reference.key())) {
                return FlagHolders.NEXT_UPDATE;
            }
        }
        return null;
    }
}
