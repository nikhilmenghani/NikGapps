# NikGapps icon artwork

`nikgapps-nik.png` is the approved NiK-only color artwork with the map pin replacing the i dot. It retains the custom lettering from the original NikGapps logo.

To regenerate Android assets, install Pillow and run:

```sh
python branding/generate_icons.py
```

The generator exports density-specific adaptive foregrounds and black backgrounds, monochrome themed icons, legacy square/round launcher icons, the in-app logo, and a white notification silhouette. The foreground is centered within a 108 dp layer with a maximum artwork extent of 60 dp to allow launcher masks.

`launcher-preview.png` shows circular, rounded-square and themed previews. `play-store-icon.png` is an opaque black 512 px square export without baked-in corner masking.
