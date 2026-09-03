# App icon provenance

The canonical owner-approved My Cycle app icon is the original raster PNG named `Нежный календарь с цветочным акцентом.png`.

- Canonical source dimensions: 1254×1254
- Canonical source SHA-256: `cc9fe5e688503682ce06f1b7e4e77a1a510635152185476b901b2af2a5f40cac`

Android launcher resources are full-frame raster derivatives of that source, following the same density-specific raster approach used by Password Generator. They were created only by resizing the approved image and encoding the resized raster as WebP; there is no tracing, vectorization, redrawing, restyling, cropping, added padding or layer reconstruction.

| Density | Dimensions | Path | SHA-256 |
| --- | ---: | --- | --- |
| mdpi | 48×48 | `app/src/main/res/mipmap-mdpi/ic_launcher.webp` | `26cf62dc67089b636df03edd7ed1c60f0db46ae7d77460c085e4069f8fe9542b` |
| hdpi | 72×72 | `app/src/main/res/mipmap-hdpi/ic_launcher.webp` | `47ab8d3efa5cf879355dac94354ef813f0d008ca4f9f2ad9421e5e0d05e27145` |
| xhdpi | 96×96 | `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` | `13b5555a25333b6a1fdf7c1673dbaa68989a667fa1f313156207dbdb22abf46d` |
| xxhdpi | 144×144 | `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` | `84db05a886175b67c23d45a99c3fbfe20a45166151711085e08ddd4f126b0afa` |
| xxxhdpi | 192×192 | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` | `feb0df2480c3826a5f17362279aea9974d30d6ad44b4852f460909be3028c8dd` |

The regular and round launcher references both resolve to the same density-specific raster artwork so Android does not need to upscale a single low-density asset. The removed VectorDrawable/adaptive-icon reconstruction must not be restored. CI verifies all five raster checksums and rejects the old launcher resource paths.

The canonical 1254×1254 source bytes are retained outside this repository until the repository connector can upload that binary losslessly; its SHA-256 above is the integrity reference. A future repository update may add that exact PNG byte-for-byte, but it must not alter the visible launcher artwork.
