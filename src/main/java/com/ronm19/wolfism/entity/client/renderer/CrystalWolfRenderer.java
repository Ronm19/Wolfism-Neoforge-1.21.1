package com.ronm19.wolfism.entity.client.renderer;

// keep your real package line here

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.CrystalWolfEyesLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class CrystalWolfRenderer extends WolfRenderer {

    private static final ResourceLocation CRYSTAL_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    WolfismMod.MOD_ID,
                    "textures/entity/crystal_wolf/crystal_wolf.png"
            );

    public CrystalWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new CrystalWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( @NotNull Wolf wolf) {
        return CRYSTAL_WOLF_TEXTURE;
    }
}