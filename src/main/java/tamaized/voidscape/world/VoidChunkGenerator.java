package tamaized.voidscape.world;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.NoiseSlider;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VoidChunkGenerator extends NoiseBasedChunkGenerator {

	public static final Codec<VoidChunkGenerator> codec = RecordCodecBuilder.create((p_236091_0_) -> commonCodec(p_236091_0_).and(p_236091_0_.
			group(RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).
							forGetter((p_188716_) -> p_188716_.noises),

					BiomeSource.CODEC.
							fieldOf("biome_source").
							forGetter(ChunkGenerator::getBiomeSource),

					Codec.LONG.
							fieldOf("seed").orElseGet(() -> HackyWorldGen.seed).
							forGetter(gen -> gen.seed),

					NoiseGeneratorSettings.CODEC.
							fieldOf("settings").
							forGetter(VoidChunkGenerator::getDimensionSettings))).
			apply(p_236091_0_, p_236091_0_.stable(VoidChunkGenerator::new)));

	private final Registry<NormalNoise.NoiseParameters> noises;
	private long seed;

	private VoidChunkGenerator(Registry<StructureSet> p_209106_, Registry<NormalNoise.NoiseParameters> noiseRegistry, BiomeSource biomeProvider1, long seed, Holder<NoiseGeneratorSettings> dimensionSettings) {
		super(p_209106_, noiseRegistry, biomeProvider1, seed, fixSettings(dimensionSettings));
		this.noises = noiseRegistry;
		this.seed = seed;
	}

	/**
	 * Lazy load the ASM changes
	 */
	private static Holder<NoiseGeneratorSettings> fixSettings(Holder<NoiseGeneratorSettings> settings) {
		return Holder.direct(fixSettings(settings.value()));
	}

	/**
	 * This is altered via ASM to use {@link CorrectedNoiseSettings} instead of {@link NoiseSettings}
	 */
	private static NoiseGeneratorSettings fixSettings(NoiseGeneratorSettings settings) {
		NoiseSettings s = settings.noiseSettings();
		NoiseSettings noise = new NoiseSettings(s.minY(), s.height(), s.noiseSamplingSettings(), s.topSlideSettings(), s.bottomSlideSettings(), s.noiseSizeHorizontal(), s.noiseSizeVertical(), s.terrainShaper());
		return new NoiseGeneratorSettings(noise, settings.defaultBlock(), settings.defaultFluid(), settings.noiseRouter(), settings.surfaceRule(), settings.seaLevel(), settings.disableMobGeneration(), settings.aquifersEnabled(), settings.oreVeinsEnabled(), settings.useLegacyRandomSource());
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		return codec;
	}

	@Override
	public ChunkGenerator withSeed(long seed) {
		return new VoidChunkGenerator(structureSets, noises, biomeSource.withSeed(seed), seed, getDimensionSettings());
	}

	private Holder<NoiseGeneratorSettings> getDimensionSettings() {
		return settings;
	}

	/**
	 * Basically a copy of super with changes for Y sensitivity for our 3D biome system
	 */
	@Override
	public void applyBiomeDecoration(WorldGenLevel worldGenRegion_, ChunkAccess chunk, StructureFeatureManager structureManager_) {
		int centerX = chunk.getPos().x;
		int centerZ = chunk.getPos().z;
		int x = centerX * 16;
		int z = centerZ * 16;
		int[] yIterator = new int[]{0};
		boolean cast;
		if (cast = biomeSource instanceof VoidscapeSeededBiomeProvider) {
			final int[] layers = Arrays.stream(VoidscapeSeededBiomeProvider.LAYERS).map(i -> i + 3).toArray();
			final int[] result = new int[yIterator.length + layers.length];
			System.arraycopy(yIterator, 0, result, 0, yIterator.length);
			System.arraycopy(layers, 0, result, yIterator.length, layers.length);
			yIterator = result;
		}
		WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(seed));
		long seed = rand.setDecorationSeed(worldGenRegion_.getSeed(), x, z);
		try {
			Map<Integer, List<ConfiguredStructureFeature<?, ?>>> map = worldGenRegion_.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).stream().collect(Collectors.groupingBy(structure -> structure.feature.step().ordinal()));
			List<BiomeSource.StepFeatureData> list = this.biomeSource.featuresPerStep();
			int j = list.size();
			Registry<PlacedFeature> featureRegistry = worldGenRegion_.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
			Registry<ConfiguredStructureFeature<?, ?>> structureRegistry = worldGenRegion_.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
			int k = Math.max(GenerationStep.Decoration.values().length, j);

			for (int l = 0; l < k; ++l) {

				int i1 = 0;
				if (structureManager_.shouldGenerateFeatures()) {
					for (ConfiguredStructureFeature<?, ?> structurefeature : map.getOrDefault(l, Collections.emptyList())) {
						rand.setFeatureSeed(seed, i1, l);
						Supplier<String> supplier = () -> {
							return structureRegistry.getResourceKey(structurefeature).map(Object::toString).orElseGet(structurefeature::toString);
						};

						try {
							worldGenRegion_.setCurrentlyGenerating(supplier);
							structureManager_.startsForFeature(SectionPos.of(chunk.getPos(), chunk.getMinSection()), structurefeature).forEach((p_196726_) -> {
								p_196726_.placeInChunk(worldGenRegion_, structureManager_, this, rand, writableArea(chunk), chunk.getPos());
							});
						} catch (Exception exception) {
							CrashReport crashreport1 = CrashReport.forThrowable(exception, "Feature placement");
							crashreport1.addCategory("Feature").setDetail("Description", supplier::get);
							throw new ReportedException(crashreport1);
						}

						++i1;
					}
				}

				if (l < j) {
					IntSet intset = new IntArraySet();

					for (Holder<Biome> biome : biomeSource.possibleBiomes()) {
						List<HolderSet<PlacedFeature>> list2 = biome.value().getGenerationSettings().features();
						if (l < list2.size()) {
							HolderSet<PlacedFeature> list1 = list2.get(l);
							BiomeSource.StepFeatureData biomesource$stepfeaturedata1 = list.get(l);
							list1.stream().map(Holder::value).forEach((p_196751_) -> {
								intset.add(biomesource$stepfeaturedata1.indexMapping().applyAsInt(p_196751_));
							});
						}
					}

					int j1 = intset.size();
					int[] aint = intset.toIntArray();
					Arrays.sort(aint);
					BiomeSource.StepFeatureData biomesource$stepfeaturedata = list.get(l);

					for (int k1 = 0; k1 < j1; ++k1) {
						int l1 = aint[k1];
						PlacedFeature placedfeature = biomesource$stepfeaturedata.features().get(l1);
						Supplier<String> supplier1 = () -> {
							return featureRegistry.getResourceKey(placedfeature).map(Object::toString).orElseGet(placedfeature::toString);
						};
						rand.setFeatureSeed(seed, l1, l);

						try {
							worldGenRegion_.setCurrentlyGenerating(supplier1);
							for (int y : yIterator) {
								Holder<Biome> biome = cast ? ((VoidscapeSeededBiomeProvider) biomeSource).
										getRealNoiseBiome((centerX << 2) + 2, y, (centerZ << 2) + 2) : this.biomeSource.
										getNoiseBiome((centerX << 2) + 2, (y >> 2), (centerZ << 2) + 2, this.climateSampler());
								if (biome.value().getGenerationSettings().hasFeature(placedfeature)) {
									BlockPos pos = new BlockPos(x, y, z);
									placedfeature.placeWithBiomeCheck(worldGenRegion_, this, rand, pos);
								}
							}
						} catch (Exception exception1) {
							CrashReport crashreport2 = CrashReport.forThrowable(exception1, "Feature placement");
							crashreport2.addCategory("Feature").setDetail("Description", supplier1::get);
							throw new ReportedException(crashreport2);
						}
					}
				}
			}
		} catch (Exception var14) {
			CrashReport lvt_13_1_ = CrashReport.forThrowable(var14, "Biome decoration");
			lvt_13_1_.addCategory("Generation").setDetail("CenterX", centerX).setDetail("CenterZ", centerZ).setDetail("Seed", seed);
			new ReportedException(lvt_13_1_).printStackTrace();
		}
	}

	private static BoundingBox writableArea(ChunkAccess p_187718_) { // TODO: make Y sensitive
		ChunkPos chunkpos = p_187718_.getPos();
		int i = chunkpos.getMinBlockX();
		int j = chunkpos.getMinBlockZ();
		LevelHeightAccessor levelheightaccessor = p_187718_.getHeightAccessorForGeneration();
		int k = levelheightaccessor.getMinBuildHeight() + 1;
		int l = levelheightaccessor.getMaxBuildHeight() - 1;
		return new BoundingBox(i, k, j, i + 15, l, j + 15);
	}

	/**
	 * Extends {@link NoiseSettings)} via asm
	 */
	@SuppressWarnings("unused")
	private static class CorrectedNoiseSettings {

		private final int noiseSizeHorizontal;

		private CorrectedNoiseSettings(int minY, int height, NoiseSamplingSettings noiseSamplingSettings, NoiseSlider topSlideSettings, NoiseSlider bottomSlideSettings, int noiseSizeHorizontal, int noiseSizeVertical, TerrainShaper terrainShaper) {
			this.noiseSizeHorizontal = noiseSizeHorizontal;
		}

		public int getCellWidth() {
			return noiseSizeHorizontal << 1;
		}

	}

}
