package io.papermc.generator.rewriter.registration;

import io.papermc.typewriter.replace.ReplaceOptionsLike;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import java.util.function.Function;
import org.jetbrains.annotations.Contract;

public record RewriterHolder(String pattern, SearchReplaceRewriter rewriter) {

    @Contract(value = "_, _ -> new", pure = true)
    public static RewriterHolder holder(String pattern, SearchReplaceRewriter rewriter) {
        return new RewriterHolder(pattern, rewriter);
    }

    public SearchReplaceRewriter transform(Function<String, ReplaceOptionsLike> patternMapper) {
        return this.rewriter.withOptions(patternMapper.apply(this.pattern)).customName(this.pattern);
    }
}
