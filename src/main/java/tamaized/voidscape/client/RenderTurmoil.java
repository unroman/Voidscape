package tamaized.voidscape.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.Turmoil;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Voidscape.MODID)
public class RenderTurmoil {

	static final ResourceLocation TEXTURE_MASK = new ResourceLocation(Voidscape.MODID, "textures/ui/mask.png");
	static final Color24 colorHolder = new Color24();
	private static float deltaTick;
	private static Boolean deltaPos;

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START || Minecraft.getInstance().isGamePaused() || Minecraft.getInstance().world == null)
			return;
		if (Minecraft.getInstance().player != null)
			Minecraft.getInstance().player.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapTurmoilData).ifPresent(data -> {
				if (data.getTick() <= 0)
					deltaTick = 0;
				else if (data.getTick() > deltaTick)
					deltaPos = true;
				else if (data.getTick() < deltaTick)
					deltaPos = false;
				if (deltaPos != null && data.getState() == Turmoil.State.CONSUME) {
					if (deltaPos)
						deltaTick++;
					else {
						deltaPos = null;
						deltaTick = data.getTick();
					}
				} else
					deltaTick = data.getTick();
			}));
	}

	@SubscribeEvent
	public static void render(RenderGameOverlayEvent.Pre event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.ALL)
			return;
		World world = Minecraft.getInstance().world;
		if (world != null && Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapTurmoilData).ifPresent(data -> {
				float perc = MathHelper.clamp(

						(deltaTick + event.getPartialTicks() *

								(deltaPos == null ?

										data.getState() == Turmoil.State.CONSUME ? -0.01F : 0 :

										deltaPos ? -1 : 1)

						) / data.getMaxTick(),

						0F, 1F);
				if (perc > 0) {
					RenderSystem.enableBlend();
					RenderSystem.enableAlphaTest();
					{

						BufferBuilder buffer = Tessellator.getInstance().getBuffer();
						buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);

						MainWindow window = Minecraft.getInstance().getMainWindow();

						float x = 0F;
						float y = 0F;
						float w = window.getScaledWidth();
						float h = window.getScaledHeight();
						float z = 0F;

						Consumer<Color24> verticies = color -> {
							final float r = Color24.asFloat(color.bit24);
							final float g = Color24.asFloat(color.bit16);
							final float b = Color24.asFloat(color.bit8);
							final float a = Color24.asFloat(color.bit0);
							buffer.pos(x, y + h, z).color(r, g, b, a).tex(0F, 1F).endVertex();
							buffer.pos(x + w, y + h, z).color(r, g, b, a).tex(1F, 1F).endVertex();
							buffer.pos(x + w, y, z).color(r, g, b, a).tex(1F, 0F).endVertex();
							buffer.pos(x, y, z).color(r, g, b, a).tex(0F, 0F).endVertex();
						};
						verticies.accept(colorHolder.set(1F, 1F, 1F, 1F));

						Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE_MASK);

						final int stencilIndex = 10;

						StencilBufferUtil.setup(stencilIndex, () -> {
							RenderSystem.alphaFunc(GL11.GL_LESS, perc);
							Tessellator.getInstance().draw();
							RenderSystem.defaultAlphaFunc();
						});

						buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
						verticies.accept(colorHolder.set(0F, 0F, 0F, 1F));

						StencilBufferUtil.render(stencilIndex, () -> {
							RenderSystem.disableTexture();
							Tessellator.getInstance().draw();
							RenderSystem.enableTexture();
						}, true);
					}
					RenderSystem.disableAlphaTest();
					RenderSystem.disableBlend();
				}
			}));
		}
		OverlayMessageHandler.render(event.getMatrixStack(), event.getPartialTicks());
	}

	static class Color24 {

		public int bit24;
		public int bit16;
		public int bit8;
		public int bit0;

		public static float asFloat(int value) {
			return value / 255F;
		}

		public static int asInt(float value) {
			return (int) (value * 255);
		}

		public int packed() {
			return (bit24 << 24) | (bit16 << 16) | (bit8 << 8) | bit0;
		}

		public Color24 unpack(int packed) {
			return set(

					(packed >> 24) & 0xFF,

					(packed >> 16) & 0xFF,

					(packed >> 8) & 0xFF,

					packed & 0xFF

			);
		}

		public Color24 set(int b24, int b16, int b8, int b0) {
			bit0 = b0;
			bit8 = b8;
			bit16 = b16;
			bit24 = b24;
			return this;
		}

		public Color24 set(float b24, float b16, float b8, float b0) {
			set(asInt(b24), asInt(b16), asInt(b8), asInt(b0));
			return this;
		}

	}

}
