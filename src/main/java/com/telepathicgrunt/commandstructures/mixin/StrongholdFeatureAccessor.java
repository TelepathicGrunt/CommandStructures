package com.telepathicgrunt.commandstructures.mixin;

import net.minecraft.world.level.levelgen.feature.StrongholdFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StrongholdFeature.class)
public interface StrongholdFeatureAccessor {
    @Invoker("generatePieces")
    static void callGeneratePieces(StructurePiecesBuilder structurePiecesBuilder, PieceGenerator.Context<NoneFeatureConfiguration> context) {
        throw new UnsupportedOperationException();
    }
}
