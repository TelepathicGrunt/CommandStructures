package com.telepathicgrunt.commandstructures.mixin;

import net.minecraft.world.level.levelgen.feature.MineshaftFeature;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MineshaftFeature.class)
public interface MineshaftFeatureAccessor {
    @Invoker("generatePieces")
    static void callGeneratePieces(StructurePiecesBuilder structurePiecesBuilder, PieceGenerator.Context<MineshaftConfiguration> context) {
        throw new UnsupportedOperationException();
    }
}
