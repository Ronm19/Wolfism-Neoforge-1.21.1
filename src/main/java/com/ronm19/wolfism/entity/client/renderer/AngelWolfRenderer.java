package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.AngelWolfEyesLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class AngelWolfRenderer extends WolfRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/angel_wolf/angel_wolf.png");

    public AngelWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new AngelWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( @NotNull Wolf entity) {
        return TEXTURE;
    }

    @Override
    public void render( @NotNull Wolf entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}