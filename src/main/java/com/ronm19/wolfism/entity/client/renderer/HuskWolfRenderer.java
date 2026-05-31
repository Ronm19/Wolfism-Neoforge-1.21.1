package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.HuskWolfEyesLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class HuskWolfRenderer extends WolfRenderer {

    private static final ResourceLocation HUSK_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/husk_wolf/husk_wolf.png");

    public HuskWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new HuskWolfEyesLayer(this));

    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return HUSK_WOLF_TEXTURE;
    }
}