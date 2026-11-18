# Dragonfruit Powers (Fabric 1.20.1)

Eat a Dragonfruit, get anime-style powers. The visuals are the main feature: a **stretching rubber arm you see in first person** (it uses your own skin, wobbles, thins out as it stretches, bulges at the fist, and snaps back), fire fists, ice crystal eruptions, light beams, and cracks in the air with screen shake.

- Minecraft **1.20.1**, Fabric Loader **0.16.9** (works on 0.15+)
- Requires **Fabric API** for 1.20.1 (0.92.x)
- Java 17

## Install (Android launcher)
1. Put **two jars** in your instance's `mods` folder:
   - `dragonfruit-powers-0.1.0.jar` (from the GitHub Actions build, see below)
   - Fabric API for 1.20.1, e.g. `fabric-api-0.92.2+1.20.1.jar` (Modrinth / CurseForge)
2. Launch the `1.20.1 Fabric 0.16.9` profile.
3. Creative inventory → **Dragonfruit Powers** tab, or find fruits in treasure chests.

## Controls
| Key | Action |
|---|---|
| **R** | Use selected ability |
| **C** | Next ability |
| **X** | Previous ability |

You can rebind them in Options → Controls → Dragonfruit Powers.
**On a phone:** add custom-control buttons in your launcher mapped to the R, C and X keys. Only two are needed (R + C).

## The fruits
| Fruit | Abilities | Passive |
|---|---|---|
| **Rubber** | Rubber Pistol (stretch punch, 26 blocks), Rubber Rocket (both arms grab a block, slingshot you), Rubber Gatling (storm of arms), Second Gear (steam, speed, strength, pink skin, faster/stronger pistol) | No fall damage (you bounce), immune to lightning, arrows bounce off |
| **Flame** | Fire Fist (giant fireball), Flame Pillar (ring of fire columns), Firefly Swarm (homing embers), Flame Body (melee passes through you and burns attackers) | Fire immune |
| **Ice** | Ice Spears (7-spear volley), Ice Age (freezes water and erupts ice crystals in 12 blocks), Ice Coffin (encase a target in ice, then shatter it) | Walk on water (frost walker), freeze immune |
| **Light** | Light Flash (teleport 40 blocks, damages everything in the path), Light Barrage (laser bullets), Light Cannon (charge, then giant beam) | No fall damage |
| **Quake** | Quake Punch (cracks the air, shockwave cone), Ground Quake (AoE knock-up), Quake Bubble (charge, then huge shockwave) | Explosion immune |

**Every fruit:** water drains you. You sink, get weakened, and can't use abilities while touching water. Eating a different fruit swaps your power. Powers persist through death and relogs.

Fire Fist only sets blocks on fire if the `mobGriefing` gamerule is on. Nothing else breaks blocks (Ice Age turns surface water into ice that melts).

## Commands (op)
- `/dragonfruit set <rubber|flame|ice|light|quake>`
- `/dragonfruit clear`

## Build from your phone (GitHub Actions)
1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Pushing starts the **Build mod JAR** workflow automatically (or run it from the Actions tab).
3. When it goes green, open the repo's **Releases** (or the direct link `https://github.com/nothatcher-creator/Dragonfruit-Power/releases/latest/download/dragonfruit-powers.jar`) and download `dragonfruit-powers.jar` straight into `mods`. No unzipping needed.

If the build fails, the log under the **Build** step shows the error. Send it to Claude to fix.

## Adding more fruits
1. `FruitType.java`: add an entry: id, colour, abilities with cooldowns (ticks).
2. `power/Abilities.java`: add a `case` in `run()` and write the ability methods. Reuse the helpers (`rayBlock`, `rayEntity`, `cone`, `inRadius`, `hurt`, `push`, `launch`, `simulate`) and the visuals in `net/Vfx.java` (arm, beam, ring, orb, spikes, air crack, projectile, shake, flash, burst...).
3. Assets: `textures/item/<id>_dragonfruit.png` (+ `.mcmeta` for animation), `models/item/<id>_dragonfruit.json`, and lang lines (`item.`, `fruit.`, `tooltip.`, `ability.`).
   `tools/make_textures.py` regenerates the animated fruit textures; add a palette entry for the new fruit.
4. New visual types: add a constant + sender in `net/Vfx.java`, a `case` in `client/vfx/VfxDecoder.java`, and a `VfxEffect` subclass.

## Code map
```
DragonfruitMod / ModItems / DragonfruitItem / ModLoot / ModCommands / FruitType
power/   PowerState (saved), PowerManager (cooldowns, passives, damage rules), Abilities, Scheduler
net/     Net (packets), Vfx (server -> client effect senders)
client/  DragonfruitClient (keys, packets), Hud, CameraFx (shake/FOV/flash), ClientPowerData
client/vfx/  ArmEffect (rubber arm), GatlingEffect, BeamEffect, RingEffect, OrbEffect, SpikesEffect,
             IceCoffinEffect, AirCrackEffect, ProjectileEffect, FireflyEffect, AuraEffect, Particles, Gfx, Poses
mixin/client/ GameRendererMixin (FOV + shake), HeldItemRendererMixin (hides vanilla hand while stretched)
```

## Verification status
- Java syntax and language-level checks pass. The Minecraft/Fabric method names, parameter lists and mixin targets were checked against the official Yarn 1.20.1+build.10 mappings and the Fabric API 0.92.2+1.20.1 source.
- **Not yet done:** a full Gradle compile (runs in GitHub Actions) and in-game testing on your device.
