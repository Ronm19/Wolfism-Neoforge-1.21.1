package com.ronm19.wolfism.entity.client.renderer;


import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.WitheredWolfEyesLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class WitheredWolfRenderer extends WolfRenderer {
    private static final ResourceLocation WITHERED_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/withered_wolf/withered_wolf.png");

    public WitheredWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new WitheredWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return WITHERED_WOLF_TEXTURE;
    }
}