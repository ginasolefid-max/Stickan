import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Volume2, VolumeX, Pause, Play, RotateCcw, Shield, Settings as SettingsIcon, ShoppingBag, ArrowUpCircle } from 'lucide-react';

type GameState = 'TITLE_MENU' | 'PLAYING' | 'PAUSED' | 'VICTORY' | 'GAME_OVER';
type GameMode = 'CAMPAIGN' | 'ENDLESS' | 'BOSS_RUSH' | 'TIME_ATTACK';
type ArrowType = 'NORMAL' | 'FIRE' | 'ICE' | 'EXPLOSIVE' | 'PIERCING' | 'POISON' | 'TRIPLE';
type ZombieType = 'WALKER' | 'RUNNER' | 'TANK' | 'JUMPER' | 'SPITTER' | 'EXPLODER' | 'BOSS';

interface Arrow {
  x: number;
  y: number;
  px: number;
  py: number;
  vx: number;
  vy: number;
  type: ArrowType;
  damage: number;
  stuck: boolean;
  stuckZombieId: number | null;
  stuckOffX: number;
  stuckOffY: number;
  angle: number;
  life: number;
  pierce: number;
  hitIds: number[];
  spawnX: number;
}

interface Zombie {
  id: number;
  type: ZombieType;
  x: number;
  y: number;
  vy: number;
  hp: number;
  maxHp: number;
  speed: number;
  scale: number;
  walk: number;
  atkCd: number;
  flash: number;
  airborne: boolean;
  jumpCd: number;
  spitCd: number;
  burn: number;
  freeze: number;
  poison: number;
  legSlow: number;
  dotAcc: number;
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  life: number;
  maxLife: number;
  color: string;
  grav: number;
}

interface Popup {
  text: string;
  x: number;
  y: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  large: boolean;
}

interface Acid {
  x: number;
  y: number;
  vx: number;
  vy: number;
  dmg: number;
}

interface SaveState {
  coins: number;
  gems: number;
  highestLevel: number;
  damageRank: number;
  drawRank: number;
  healthRank: number;
  eagleEyeRank: number;
  barricadeRank: number;
  bloodEnabled: boolean;
  trajectoryPreview: boolean;
  leftHanded: boolean;
  sfxEnabled: boolean;
  ammo: Record<Exclude<ArrowType, 'NORMAL'>, number>;
  bestEndless: number;
}

const W = 1200;
const H = 640;
const GROUND_Y = 540;
const GRAVITY = 960;
const PLAYER_X = 110;
const BARRICADE_X = 215;

const ARROW_INFO: Record<ArrowType, { label: string; mult: number; color: string; cost: number; pack: number }> = {
  NORMAL: { label: 'Normal', mult: 1.0, color: '#e2c280', cost: 0, pack: 999 },
  FIRE: { label: 'Fire', mult: 1.25, color: '#fb923c', cost: 80, pack: 10 },
  ICE: { label: 'Ice', mult: 1.1, color: '#38bdf8', cost: 85, pack: 10 },
  EXPLOSIVE: { label: 'Blast', mult: 1.6, color: '#ef4444', cost: 130, pack: 6 },
  PIERCING: { label: 'Pierce', mult: 1.35, color: '#e2e8f0', cost: 110, pack: 8 },
  POISON: { label: 'Poison', mult: 1.15, color: '#4ade80', cost: 95, pack: 8 },
  TRIPLE: { label: 'Triple', mult: 0.78, color: '#facc15', cost: 120, pack: 6 },
};

const STORAGE_KEY = 'stickman_archer_siege_v1';

const defaultSave = (): SaveState => ({
  coins: 200,
  gems: 5,
  highestLevel: 1,
  damageRank: 0,
  drawRank: 0,
  healthRank: 0,
  eagleEyeRank: 0,
  barricadeRank: 0,
  bloodEnabled: false,
  trajectoryPreview: true,
  leftHanded: false,
  sfxEnabled: true,
  ammo: { FIRE: 6, ICE: 6, EXPLOSIVE: 3, PIERCING: 4, POISON: 4, TRIPLE: 3 },
  bestEndless: 0,
});

function loadSave(): SaveState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return defaultSave();
    return { ...defaultSave(), ...JSON.parse(raw) };
  } catch {
    return defaultSave();
  }
}

export default function App() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);

  const [save, setSave] = useState<SaveState>(() => loadSave());
  const [gameState, setGameState] = useState<GameState>('TITLE_MENU');
  const [mode, setMode] = useState<GameMode>('CAMPAIGN');
  const [level, setLevel] = useState(1);
  const [selectedArrow, setSelectedArrow] = useState<ArrowType>('NORMAL');
  const [activeModal, setActiveModal] = useState<'NONE' | 'UPGRADES' | 'SHOP' | 'SETTINGS'>('NONE');

  const [hud, setHud] = useState({
    hp: 100,
    maxHp: 100,
    wallHp: 220,
    maxWallHp: 220,
    wave: 1,
    totalWaves: 3,
    wind: 0,
    score: 0,
    kills: 0,
    sessionCoins: 0,
    combo: 0,
    timeLeft: 90,
    bossHp: 0,
    bossMaxHp: 0,
    stars: 0,
  });

  const saveRef = useRef(save);
  saveRef.current = save;
  const arrowChoiceRef = useRef(selectedArrow);
  arrowChoiceRef.current = selectedArrow;

  const worldRef = useRef({
    arrows: [] as Arrow[],
    zombies: [] as Zombie[],
    particles: [] as Particle[],
    popups: [] as Popup[],
    acids: [] as Acid[],
    hp: 100,
    maxHp: 100,
    wallHp: 220,
    maxWallHp: 220,
    wave: 1,
    totalWaves: 3,
    wind: 0,
    score: 0,
    kills: 0,
    sessionCoins: 0,
    combo: 0,
    comboTimer: 0,
    timeLeft: 90,
    toSpawn: 0,
    bossPending: false,
    spawnTimer: 0.6,
    nextZId: 1,
    wavePause: 0,
    dragging: false,
    dragStartX: 0,
    dragStartY: 0,
    dragCurrX: 0,
    dragCurrY: 0,
    aimAngle: -0.35,
    power: 0,
    drawCd: 0,
    bulletTime: 0,
    bulletCd: 0,
    shake: 0,
    bannerText: '',
    bannerTimer: 0,
    state: 'TITLE_MENU' as GameState,
    mode: 'CAMPAIGN' as GameMode,
    level: 1,
  });

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(save));
    } catch {
      // Ignore storage quota errors
    }
  }, [save]);

  const playSfx = useCallback((kind: 'shoot' | 'head' | 'hit' | 'boom' | 'clear') => {
    if (!saveRef.current.sfxEnabled) return;
    try {
      const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!audioCtxRef.current) audioCtxRef.current = new Ctx();
      const ctx = audioCtxRef.current;
      if (ctx.state === 'suspended') void ctx.resume();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      const now = ctx.currentTime;
      if (kind === 'shoot') {
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(340, now);
        osc.frequency.exponentialRampToValueAtTime(110, now + 0.11);
        gain.gain.setValueAtTime(0.12, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.11);
      } else if (kind === 'head') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880, now);
        osc.frequency.exponentialRampToValueAtTime(420, now + 0.16);
        gain.gain.setValueAtTime(0.18, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.16);
      } else if (kind === 'boom') {
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(120, now);
        osc.frequency.exponentialRampToValueAtTime(36, now + 0.28);
        gain.gain.setValueAtTime(0.2, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.28);
      } else if (kind === 'clear') {
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(523, now);
        osc.frequency.setValueAtTime(659, now + 0.08);
        osc.frequency.setValueAtTime(784, now + 0.16);
        gain.gain.setValueAtTime(0.14, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.32);
      } else {
        osc.type = 'square';
        osc.frequency.setValueAtTime(170, now);
        osc.frequency.exponentialRampToValueAtTime(70, now + 0.08);
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.08);
      }
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.35);
    } catch {
      // Audio unavailable
    }
  }, []);

  const setupWave = useCallback((waveNum: number, curMode: GameMode, curLevel: number) => {
    const w = worldRef.current;
    w.wave = waveNum;
    const windRange = Math.min(130, 22 + curLevel * 3 + waveNum * 8);
    w.wind = (Math.random() * 2 - 1) * windRange;
    w.toSpawn = curMode === 'BOSS_RUSH' ? 3 + waveNum : 5 + curLevel + waveNum * 2;
    w.bossPending = curMode === 'BOSS_RUSH' || (curMode === 'CAMPAIGN' && waveNum === w.totalWaves && curLevel % 5 === 0) || (curMode === 'ENDLESS' && waveNum % 5 === 0);
    w.spawnTimer = 0.5;
    w.bannerText = `WAVE ${waveNum}`;
    w.bannerTimer = 2.0;
  }, []);

  const startMatch = useCallback((chosenMode: GameMode, chosenLevel: number) => {
    const s = saveRef.current;
    const w = worldRef.current;
    const maxHp = 100 + s.healthRank * 25;
    const maxWall = 220 + s.healthRank * 40;
    w.arrows = [];
    w.zombies = [];
    w.particles = [];
    w.popups = [];
    w.acids = [];
    w.hp = maxHp;
    w.maxHp = maxHp;
    w.wallHp = maxWall;
    w.maxWallHp = maxWall;
    w.totalWaves = chosenMode === 'CAMPAIGN' ? (chosenLevel >= 5 ? 4 : 3) : 999;
    w.score = 0;
    w.kills = 0;
    w.sessionCoins = 0;
    w.combo = 0;
    w.comboTimer = 0;
    w.timeLeft = 90;
    w.bulletTime = 0;
    w.bulletCd = 0;
    w.shake = 0;
    w.dragging = false;
    w.power = 0;
    w.mode = chosenMode;
    w.level = chosenLevel;
    w.state = 'PLAYING';
    setupWave(1, chosenMode, chosenLevel);
    setMode(chosenMode);
    setLevel(chosenLevel);
    setActiveModal('NONE');
    setGameState('PLAYING');
  }, [setupWave]);

  const repairWall = () => {
    const w = worldRef.current;
    if (save.coins < 40 || w.wallHp >= w.maxWallHp || w.state !== 'PLAYING') return;
    setSave((prev) => ({ ...prev, coins: prev.coins - 40 }));
    w.wallHp = Math.min(w.maxWallHp, w.wallHp + w.maxWallHp * 0.45);
    w.popups.push({ text: 'WALL REPAIRED', x: BARRICADE_X, y: GROUND_Y - 120, vy: -45, life: 1, maxLife: 1, color: '#38bdf8', large: true });
  };

  useEffect(() => {
    let raf = 0;
    let last = performance.now();

    const spawnParticles = (x: number, y: number, color: string, count: number, boost = 1) => {
      const w = worldRef.current;
      for (let i = 0; i < count; i++) {
        const a = Math.random() * Math.PI * 2;
        const spd = (50 + Math.random() * 170) * boost;
        w.particles.push({
          x,
          y,
          vx: Math.cos(a) * spd,
          vy: Math.sin(a) * spd - 60,
          size: 3 + Math.random() * 4,
          life: 0.45 + Math.random() * 0.25,
          maxLife: 0.7,
          color,
          grav: 520,
        });
      }
    };

    const damageZombie = (z: Zombie, dmg: number, headshot: boolean, pierce: boolean) => {
      const w = worldRef.current;
      const actual = z.type === 'TANK' && !pierce ? dmg * 0.68 : dmg;
      z.hp -= actual;
      z.flash = 0.14;
      w.popups.push({
        text: `-${Math.round(actual)}`,
        x: z.x - 12,
        y: z.y - 72 * z.scale,
        vy: -44,
        life: 0.7,
        maxLife: 0.7,
        color: headshot ? '#facc15' : '#f8fafc',
        large: false,
      });
      if (z.hp <= 0) {
        w.kills += 1;
        const reward = z.type === 'BOSS' ? 120 : z.type === 'TANK' ? 22 : 10;
        const pts = z.type === 'BOSS' ? 400 : z.type === 'TANK' ? 75 : 30;
        w.sessionCoins += reward;
        w.score += pts + w.combo * 5;
        if (z.type === 'EXPLODER') {
          playSfx('boom');
          w.shake = 0.25;
          spawnParticles(z.x, z.y - 35, '#f97316', 22, 1.5);
          for (const other of w.zombies) {
            if (other.id !== z.id && Math.abs(other.x - z.x) < 130) {
              other.hp -= 45;
            }
          }
        }
      }
    };

    const tick = (now: number) => {
      const rawDt = Math.min(0.05, (now - last) / 1000);
      last = now;
      const w = worldRef.current;
      const s = saveRef.current;

      if (w.state === 'PLAYING') {
        if (w.drawCd > 0) w.drawCd -= rawDt;
        if (w.bannerTimer > 0) w.bannerTimer -= rawDt;
        if (w.bulletCd > 0) w.bulletCd -= rawDt;
        if (w.shake > 0) w.shake -= rawDt;
        if (w.combo > 0) {
          w.comboTimer -= rawDt;
          if (w.comboTimer <= 0) w.combo = 0;
        }

        if (w.mode === 'TIME_ATTACK') {
          w.timeLeft -= rawDt;
          if (w.timeLeft <= 0) {
            w.timeLeft = 0;
            w.state = 'VICTORY';
            playSfx('clear');
            setSave((prev) => ({ ...prev, coins: prev.coins + w.sessionCoins }));
            setGameState('VICTORY');
          }
        }

        let simDt = rawDt;
        if (w.bulletTime > 0) {
          w.bulletTime -= rawDt;
          simDt = rawDt * 0.24;
        }

        // Spawn zombies
        if (w.toSpawn > 0 || w.bossPending) {
          w.spawnTimer -= simDt;
          if (w.spawnTimer <= 0) {
            w.spawnTimer = Math.max(0.55, 1.55 - w.wave * 0.08) * (0.8 + Math.random() * 0.4);
            let zType: ZombieType = 'WALKER';
            if (w.toSpawn > 0) {
              w.toSpawn -= 1;
              const r = Math.random();
              if (w.wave + w.level >= 4) {
                if (r < 0.26) zType = 'WALKER';
                else if (r < 0.48) zType = 'RUNNER';
                else if (r < 0.64) zType = 'TANK';
                else if (r < 0.78) zType = 'JUMPER';
                else if (r < 0.90) zType = 'SPITTER';
                else zType = 'EXPLODER';
              } else {
                zType = r < 0.7 ? 'WALKER' : 'RUNNER';
              }
            } else {
              w.bossPending = false;
              zType = 'BOSS';
              w.bannerText = 'BOSS APPROACHING!';
              w.bannerTimer = 2.2;
            }
            const scaleHp = 1 + (w.level - 1) * 0.1 + (w.wave - 1) * 0.14;
            const baseHp: Record<ZombieType, number> = {
              WALKER: 46,
              RUNNER: 30,
              TANK: 165,
              JUMPER: 52,
              SPITTER: 44,
              EXPLODER: 58,
              BOSS: 680,
            };
            const baseSpd: Record<ZombieType, number> = {
              WALKER: 52,
              RUNNER: 122,
              TANK: 34,
              JUMPER: 84,
              SPITTER: 44,
              EXPLODER: 72,
              BOSS: 28,
            };
            const zScale: Record<ZombieType, number> = {
              WALKER: 1,
              RUNNER: 0.94,
              TANK: 1.24,
              JUMPER: 0.98,
              SPITTER: 1,
              EXPLODER: 1.08,
              BOSS: 1.72,
            };
            w.zombies.push({
              id: w.nextZId++,
              type: zType,
              x: W + 60,
              y: GROUND_Y,
              vy: 0,
              hp: baseHp[zType] * scaleHp,
              maxHp: baseHp[zType] * scaleHp,
              speed: baseSpd[zType],
              scale: zScale[zType],
              walk: Math.random() * 6,
              atkCd: 0.4,
              flash: 0,
              airborne: false,
              jumpCd: 2.0,
              spitCd: 2.5,
              burn: 0,
              freeze: 0,
              poison: 0,
              legSlow: 0,
              dotAcc: 0,
            });
          }
        } else if (w.zombies.length === 0) {
          if (w.mode === 'CAMPAIGN' && w.wave >= w.totalWaves) {
            w.state = 'VICTORY';
            playSfx('clear');
            const reward = 80 + w.level * 25;
            w.sessionCoins += reward;
            setSave((prev) => ({
              ...prev,
              coins: prev.coins + w.sessionCoins,
              gems: prev.gems + 2,
              highestLevel: Math.max(prev.highestLevel, w.level + 1),
            }));
            setGameState('VICTORY');
          } else {
            w.wavePause += rawDt;
            if (w.wavePause >= 1.4) {
              w.wavePause = 0;
              setupWave(w.wave + 1, w.mode, w.level);
            }
          }
        }

        // Update arrows
        for (let i = w.arrows.length - 1; i >= 0; i--) {
          const a = w.arrows[i];
          if (a.stuck) {
            a.life -= simDt;
            if (a.stuckZombieId !== null) {
              const host = w.zombies.find((z) => z.id === a.stuckZombieId);
              if (host) {
                a.x = host.x + a.stuckOffX;
                a.y = host.y + a.stuckOffY;
              } else {
                a.life = 0;
              }
            }
            if (a.life <= 0) w.arrows.splice(i, 1);
            continue;
          }

          a.px = a.x;
          a.py = a.y;
          a.vx += w.wind * simDt;
          a.vy += GRAVITY * simDt;
          a.x += a.vx * simDt;
          a.y += a.vy * simDt;
          a.angle = Math.atan2(a.vy, a.vx);

          // Bullet-time check
          if (w.bulletTime <= 0 && w.bulletCd <= 0 && a.x - a.spawnX > 520) {
            for (const z of w.zombies) {
              const hy = z.y - 80 * z.scale;
              if (Math.hypot(z.x - a.x, hy - a.y) < 115) {
                w.bulletTime = 0.65;
                w.bulletCd = 2.2;
                break;
              }
            }
          }

          // Hit zombie
          let consumed = false;
          for (const z of w.zombies) {
            if (a.hitIds.includes(z.id)) continue;
            const headX = z.x - 4 * z.scale;
            const headY = z.y - 82 * z.scale;
            const headR = 16 * z.scale;
            const inHead = Math.hypot(a.x - headX, a.y - headY) <= headR + 4;
            const inBody = Math.abs(a.x - z.x) <= 18 * z.scale && a.y >= z.y - 68 * z.scale && a.y <= z.y - 28 * z.scale;
            const inLegs = Math.abs(a.x - z.x) <= 18 * z.scale && a.y > z.y - 28 * z.scale && a.y <= z.y + 4;

            if (inHead || inBody || inLegs) {
              a.hitIds.push(z.id);
              const mult = inHead ? 3.0 : inLegs ? 0.5 : 1.0;
              if (inLegs) z.legSlow = 2.5;
              if (a.type === 'FIRE') z.burn = 4.0;
              if (a.type === 'ICE') z.freeze = 3.2;
              if (a.type === 'POISON') z.poison = 5.0;

              if (inHead) {
                w.combo += 1;
                w.comboTimer = 4.5;
                playSfx('head');
                w.shake = 0.14;
                w.popups.push({
                  text: w.combo > 1 ? `HEADSHOT! x${w.combo}` : 'HEADSHOT!',
                  x: z.x - 42,
                  y: z.y - 108 * z.scale,
                  vy: -60,
                  life: 0.95,
                  maxLife: 0.95,
                  color: '#facc15',
                  large: true,
                });
              } else {
                playSfx('hit');
              }

              const hitCol = s.bloodEnabled ? '#dc2626' : ARROW_INFO[a.type].color;
              spawnParticles(a.x, a.y, hitCol, inHead ? 14 : 8, inHead ? 1.3 : 1.0);

              if (a.type === 'EXPLOSIVE') {
                playSfx('boom');
                w.shake = 0.24;
                spawnParticles(a.x, a.y, '#f97316', 24, 1.5);
                for (const oz of w.zombies) {
                  if (Math.hypot(oz.x - a.x, (oz.y - 40) - a.y) < 130) {
                    damageZombie(oz, a.damage * 0.85, false, true);
                  }
                }
                w.arrows.splice(i, 1);
                consumed = true;
                break;
              } else {
                damageZombie(z, a.damage * mult, inHead, a.type === 'PIERCING');
                if (a.pierce > 0) {
                  a.pierce -= 1;
                } else {
                  a.stuck = true;
                  a.life = 3.5;
                  a.stuckZombieId = z.id;
                  a.stuckOffX = a.x - z.x;
                  a.stuckOffY = a.y - z.y;
                  consumed = true;
                  break;
                }
              }
            }
          }
          if (consumed) continue;

          if (a.y >= GROUND_Y) {
            if (a.type === 'EXPLOSIVE') {
              playSfx('boom');
              w.shake = 0.24;
              spawnParticles(a.x, GROUND_Y - 4, '#f97316', 24, 1.5);
              for (const oz of w.zombies) {
                if (Math.hypot(oz.x - a.x, (oz.y - 40) - GROUND_Y) < 130) {
                  damageZombie(oz, a.damage * 0.85, false, true);
                }
              }
              w.arrows.splice(i, 1);
            } else {
              a.y = GROUND_Y;
              a.stuck = true;
              a.life = 4.5;
            }
          } else if (a.x > W + 120 || a.x < -120) {
            w.arrows.splice(i, 1);
          }
        }

        // Update zombies
        for (let i = w.zombies.length - 1; i >= 0; i--) {
          const z = w.zombies[i];
          if (z.hp <= 0) {
            spawnParticles(z.x, z.y - 40 * z.scale, s.bloodEnabled ? '#b91c1c' : '#64748b', 10);
            w.zombies.splice(i, 1);
            continue;
          }
          if (z.flash > 0) z.flash -= simDt;
          if (z.atkCd > 0) z.atkCd -= simDt;
          if (z.freeze > 0) z.freeze -= simDt;
          if (z.legSlow > 0) z.legSlow -= simDt;
          if (z.burn > 0) z.burn -= simDt;
          if (z.poison > 0) z.poison -= simDt;
          if (z.jumpCd > 0) z.jumpCd -= simDt;
          if (z.spitCd > 0) z.spitCd -= simDt;

          z.dotAcc += simDt;
          if (z.dotAcc >= 0.5) {
            z.dotAcc -= 0.5;
            if (z.burn > 0) damageZombie(z, 8, false, true);
            if (z.poison > 0) damageZombie(z, 6.5, false, true);
          }

          if (z.airborne) {
            z.vy += GRAVITY * simDt;
            z.y += z.vy * simDt;
            if (z.y >= GROUND_Y) {
              z.y = GROUND_Y;
              z.vy = 0;
              z.airborne = false;
            }
          }

          const stopX = w.wallHp > 0 ? BARRICADE_X + 44 : PLAYER_X + 34;
          if (z.type === 'SPITTER' && z.x < 940 && z.x > stopX + 110 && z.spitCd <= 0) {
            z.spitCd = 2.8;
            const flightT = 1.05;
            const sx = z.x - 16;
            const sy = z.y - 68;
            const vx = (stopX - sx) / flightT;
            const vy = ((GROUND_Y - 40 - sy) - 0.5 * (GRAVITY * 0.55) * flightT * flightT) / flightT;
            w.acids.push({ x: sx, y: sy, vx, vy, dmg: 12 });
          }

          if (z.x > stopX) {
            let spd = z.speed;
            if (z.freeze > 0) spd *= 0.3;
            if (z.legSlow > 0) spd *= 0.45;
            z.x -= spd * simDt;
            z.walk += spd * 0.08 * simDt;
            if (z.type === 'JUMPER' && !z.airborne && z.jumpCd <= 0 && z.x < 820) {
              z.airborne = true;
              z.vy = -380;
              z.jumpCd = 3.2;
            }
          } else if (z.type === 'EXPLODER') {
            if (w.wallHp > 0) w.wallHp = Math.max(0, w.wallHp - 36);
            else w.hp = Math.max(0, w.hp - 32);
            z.hp = 0;
            playSfx('boom');
            w.shake = 0.25;
            spawnParticles(z.x, z.y - 35, '#f97316', 22, 1.5);
          } else if (z.atkCd <= 0) {
            z.atkCd = 1.1;
            const dmg = z.type === 'BOSS' ? 28 : z.type === 'TANK' ? 18 : 10;
            if (w.wallHp > 0) {
              const red = Math.min(0.6, s.barricadeRank * 0.06);
              w.wallHp = Math.max(0, w.wallHp - dmg * (1 - red));
            } else {
              w.hp = Math.max(0, w.hp - dmg);
              w.shake = 0.15;
            }
          }
        }

        // Update acid projectiles
        for (let i = w.acids.length - 1; i >= 0; i--) {
          const ac = w.acids[i];
          ac.vy += GRAVITY * 0.55 * simDt;
          ac.x += ac.vx * simDt;
          ac.y += ac.vy * simDt;
          if (w.wallHp > 0 && ac.x <= BARRICADE_X + 36 && ac.x >= BARRICADE_X - 8 && ac.y >= GROUND_Y - 95) {
            w.wallHp = Math.max(0, w.wallHp - ac.dmg);
            w.acids.splice(i, 1);
          } else if (ac.x <= PLAYER_X + 16 && ac.y >= GROUND_Y - 100) {
            w.hp = Math.max(0, w.hp - ac.dmg);
            w.acids.splice(i, 1);
          } else if (ac.y >= GROUND_Y || ac.x < 0) {
            w.acids.splice(i, 1);
          }
        }

        if (w.hp <= 0) {
          w.hp = 0;
          w.state = 'GAME_OVER';
          setSave((prev) => ({
            ...prev,
            coins: prev.coins + w.sessionCoins,
            bestEndless: w.mode === 'ENDLESS' ? Math.max(prev.bestEndless, w.score) : prev.bestEndless,
          }));
          setGameState('GAME_OVER');
        }

        // Particles & Popups
        for (let i = w.particles.length - 1; i >= 0; i--) {
          const p = w.particles[i];
          p.life -= simDt;
          p.vy += p.grav * simDt;
          p.x += p.vx * simDt;
          p.y += p.vy * simDt;
          if (p.y >= GROUND_Y) {
            p.y = GROUND_Y;
            p.vy *= -0.35;
          }
          if (p.life <= 0) w.particles.splice(i, 1);
        }
        for (let i = w.popups.length - 1; i >= 0; i--) {
          const pp = w.popups[i];
          pp.life -= rawDt;
          pp.y += pp.vy * rawDt;
          if (pp.life <= 0) w.popups.splice(i, 1);
        }

        const boss = w.zombies.find((z) => z.type === 'BOSS');
        const stars = w.hp / w.maxHp >= 0.8 && w.wallHp > 0 ? 3 : w.hp / w.maxHp >= 0.45 ? 2 : 1;
        setHud({
          hp: Math.ceil(w.hp),
          maxHp: w.maxHp,
          wallHp: Math.ceil(w.wallHp),
          maxWallHp: w.maxWallHp,
          wave: w.wave,
          totalWaves: w.totalWaves,
          wind: Math.round(w.wind),
          score: w.score,
          kills: w.kills,
          sessionCoins: w.sessionCoins,
          combo: w.combo,
          timeLeft: Math.ceil(w.timeLeft),
          bossHp: boss ? Math.ceil(boss.hp) : 0,
          bossMaxHp: boss ? Math.ceil(boss.maxHp) : 0,
          stars,
        });
      }

      // Draw scene
      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.save();
          if (w.shake > 0) {
            ctx.translate((Math.random() - 0.5) * 10, (Math.random() - 0.5) * 10);
          }

          // Sky & hills
          const sky = ctx.createLinearGradient(0, 0, 0, GROUND_Y);
          sky.addColorStop(0, '#090d16');
          sky.addColorStop(1, '#1e293b');
          ctx.fillStyle = sky;
          ctx.fillRect(0, 0, W, GROUND_Y);

          ctx.fillStyle = '#e2e8f0';
          ctx.beginPath();
          ctx.arc(960, 120, 28, 0, Math.PI * 2);
          ctx.fill();

          ctx.fillStyle = '#0f172a';
          for (let i = 0; i < 6; i++) {
            ctx.beginPath();
            ctx.arc(i * 230 + 100, GROUND_Y + 20, 160, Math.PI, 0);
            ctx.fill();
          }

          // Ground
          ctx.fillStyle = '#182230';
          ctx.fillRect(0, GROUND_Y, W, H - GROUND_Y);
          ctx.fillStyle = '#334155';
          ctx.fillRect(0, GROUND_Y, W, 4);

          // Barricade
          if (w.wallHp > 0) {
            const wallH = 44 + 46 * (w.wallHp / w.maxWallHp);
            ctx.fillStyle = '#78350f';
            ctx.fillRect(BARRICADE_X, GROUND_Y - wallH, 34, wallH);
            ctx.strokeStyle = '#94a3b8';
            ctx.lineWidth = 4;
            ctx.beginPath();
            ctx.moveTo(BARRICADE_X + 28, GROUND_Y - wallH * 0.4);
            ctx.lineTo(BARRICADE_X + 52, GROUND_Y - wallH * 0.55);
            ctx.moveTo(BARRICADE_X + 28, GROUND_Y - wallH * 0.75);
            ctx.lineTo(BARRICADE_X + 52, GROUND_Y - wallH * 0.9);
            ctx.stroke();
          }

          // Player stickman
          ctx.strokeStyle = '#e2e8f0';
          ctx.fillStyle = '#e2e8f0';
          ctx.lineWidth = 6;
          ctx.lineCap = 'round';
          ctx.beginPath();
          ctx.moveTo(PLAYER_X, GROUND_Y - 36);
          ctx.lineTo(PLAYER_X - 12, GROUND_Y);
          ctx.moveTo(PLAYER_X, GROUND_Y - 36);
          ctx.lineTo(PLAYER_X + 12, GROUND_Y);
          ctx.moveTo(PLAYER_X, GROUND_Y - 36);
          ctx.lineTo(PLAYER_X, GROUND_Y - 72);
          ctx.stroke();
          ctx.beginPath();
          ctx.arc(PLAYER_X, GROUND_Y - 88, 12, 0, Math.PI * 2);
          ctx.fill();

          // Bow
          const aimX = Math.cos(w.aimAngle);
          const aimY = Math.sin(w.aimAngle);
          const bowX = PLAYER_X + aimX * 26;
          const bowY = GROUND_Y - 70 + aimY * 26;
          ctx.strokeStyle = '#f59e0b';
          ctx.lineWidth = 4;
          ctx.beginPath();
          ctx.arc(bowX - aimX * 10, bowY - aimY * 10, 22, w.aimAngle - 1.1, w.aimAngle + 1.1);
          ctx.stroke();

          // Dotted trajectory preview
          if (w.dragging && s.trajectoryPreview && w.power > 0.08) {
            const spd = 1120 * (0.35 + 0.65 * w.power);
            const vx = aimX * spd;
            const vy = aimY * spd;
            const steps = 14 + s.eagleEyeRank * 3;
            for (let step = 1; step <= steps; step++) {
              const t = step * 0.045;
              const px = PLAYER_X + 24 + vx * t + 0.5 * w.wind * t * t;
              const py = GROUND_Y - 70 + vy * t + 0.5 * GRAVITY * t * t;
              if (py >= GROUND_Y) break;
              ctx.fillStyle = `rgba(253, 224, 71, ${Math.max(0.15, 1 - step / (steps + 2))})`;
              ctx.beginPath();
              ctx.arc(px, py, 3.8, 0, Math.PI * 2);
              ctx.fill();
            }
          }

          // Zombies
          for (const z of w.zombies) {
            const sc = z.scale;
            const stride = Math.sin(z.walk) * 13 * sc;
            let col = z.type === 'RUNNER' ? '#ef4444' : z.type === 'TANK' ? '#64748b' : z.type === 'JUMPER' ? '#a855f7' : z.type === 'SPITTER' ? '#84cc16' : z.type === 'EXPLODER' ? '#f97316' : z.type === 'BOSS' ? '#be123c' : '#4ade80';
            if (z.freeze > 0) col = '#38bdf8';
            if (z.flash > 0) col = '#ffffff';

            ctx.strokeStyle = col;
            ctx.fillStyle = col;
            ctx.lineWidth = (z.type === 'TANK' || z.type === 'BOSS' ? 8 : 5.5) * sc;
            ctx.beginPath();
            ctx.moveTo(z.x, z.y - 34 * sc);
            ctx.lineTo(z.x - stride, z.y);
            ctx.moveTo(z.x, z.y - 34 * sc);
            ctx.lineTo(z.x + stride, z.y);
            ctx.moveTo(z.x, z.y - 34 * sc);
            ctx.lineTo(z.x - 4 * sc, z.y - 68 * sc);
            ctx.moveTo(z.x - 4 * sc, z.y - 66 * sc);
            ctx.lineTo(z.x - 26 * sc, z.y - 58 * sc);
            ctx.stroke();

            ctx.beginPath();
            ctx.arc(z.x - 4 * sc, z.y - 82 * sc, 12 * sc, 0, Math.PI * 2);
            ctx.fill();

            if (z.type !== 'BOSS' && z.hp < z.maxHp) {
              ctx.fillStyle = 'rgba(15,23,42,0.85)';
              ctx.fillRect(z.x - 20 * sc, z.y - 104 * sc, 40 * sc, 5);
              ctx.fillStyle = '#ef4444';
              ctx.fillRect(z.x - 20 * sc, z.y - 104 * sc, 40 * sc * Math.max(0, z.hp / z.maxHp), 5);
            }
          }

          // Arrows
          for (const a of w.arrows) {
            const dx = Math.cos(a.angle);
            const dy = Math.sin(a.angle);
            ctx.strokeStyle = '#e2c280';
            ctx.lineWidth = 3;
            ctx.beginPath();
            ctx.moveTo(a.x - dx * 34, a.y - dy * 34);
            ctx.lineTo(a.x, a.y);
            ctx.stroke();
            ctx.strokeStyle = ARROW_INFO[a.type].color;
            ctx.lineWidth = 4.5;
            ctx.beginPath();
            ctx.moveTo(a.x - dx * 6, a.y - dy * 6);
            ctx.lineTo(a.x + dx * 4, a.y + dy * 4);
            ctx.stroke();
          }

          // Acid
          ctx.fillStyle = '#84cc16';
          for (const ac of w.acids) {
            ctx.beginPath();
            ctx.arc(ac.x, ac.y, 7, 0, Math.PI * 2);
            ctx.fill();
          }

          // Particles
          for (const p of w.particles) {
            ctx.globalAlpha = Math.max(0, p.life / p.maxLife);
            ctx.fillStyle = p.color;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
          }
          ctx.globalAlpha = 1;

          // Popups
          ctx.textAlign = 'center';
          for (const pp of w.popups) {
            ctx.globalAlpha = Math.max(0, pp.life / pp.maxLife);
            ctx.font = pp.large ? '700 22px Oswald, sans-serif' : '700 15px JetBrains Mono, monospace';
            ctx.fillStyle = pp.color;
            ctx.fillText(pp.text, pp.x, pp.y);
          }
          ctx.globalAlpha = 1;

          if (w.bannerTimer > 0 && w.state === 'PLAYING') {
            ctx.font = '700 44px Oswald, sans-serif';
            ctx.fillStyle = '#fbbf24';
            ctx.fillText(w.bannerText, W * 0.5, 220);
          }
          ctx.restore();
        }
      }

      raf = requestAnimationFrame(tick);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [playSfx, setupWave]);

  const toCanvasCoords = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    return {
      x: ((e.clientX - rect.left) / rect.width) * W,
      y: ((e.clientY - rect.top) / rect.height) * H,
    };
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const w = worldRef.current;
    if (w.state !== 'PLAYING' || w.drawCd > 0) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    const pt = toCanvasCoords(e);
    w.dragging = true;
    w.dragStartX = pt.x;
    w.dragStartY = pt.y;
    w.dragCurrX = pt.x;
    w.dragCurrY = pt.y;
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const w = worldRef.current;
    if (!w.dragging || w.state !== 'PLAYING') return;
    const pt = toCanvasCoords(e);
    w.dragCurrX = pt.x;
    w.dragCurrY = pt.y;
    let dx = w.dragStartX - w.dragCurrX;
    const dy = w.dragStartY - w.dragCurrY;
    if (saveRef.current.leftHanded) dx = -dx;
    const dist = Math.hypot(dx, dy);
    if (dist > 4) {
      w.aimAngle = Math.max(-1.35, Math.min(0.45, Math.atan2(dy, dx)));
      w.power = Math.min(1, dist / 180);
    }
  };

  const handlePointerUp = () => {
    const w = worldRef.current;
    if (!w.dragging) return;
    w.dragging = false;
    if (w.state !== 'PLAYING' || w.power < 0.12) {
      w.power = 0;
      return;
    }

    let type = arrowChoiceRef.current;
    if (type !== 'NORMAL') {
      const rem = saveRef.current.ammo[type];
      if (rem <= 0) {
        type = 'NORMAL';
        setSelectedArrow('NORMAL');
      } else {
        setSave((prev) => ({
          ...prev,
          ammo: { ...prev.ammo, [type]: Math.max(0, prev.ammo[type as Exclude<ArrowType, 'NORMAL'>] - 1) },
        }));
      }
    }

    const s = saveRef.current;
    const spd = 1120 * (0.35 + 0.65 * w.power);
    const dmg = 28 * (1 + s.damageRank * 0.14) * ARROW_INFO[type].mult * (0.55 + 0.45 * w.power);
    const angles = type === 'TRIPLE' ? [w.aimAngle - 0.08, w.aimAngle, w.aimAngle + 0.08] : [w.aimAngle];

    for (const ang of angles) {
      w.arrows.push({
        x: PLAYER_X + 24,
        y: GROUND_Y - 70,
        px: PLAYER_X + 24,
        py: GROUND_Y - 70,
        vx: Math.cos(ang) * spd,
        vy: Math.sin(ang) * spd,
        type,
        damage: dmg,
        stuck: false,
        stuckZombieId: null,
        stuckOffX: 0,
        stuckOffY: 0,
        angle: ang,
        life: 0,
        pierce: type === 'PIERCING' ? 3 : 0,
        hitIds: [],
        spawnX: PLAYER_X + 24,
      });
    }

    w.power = 0;
    w.drawCd = Math.max(0.14, 0.34 / (1 + s.drawRank * 0.09));
    playSfx('shoot');
  };

  const buyUpgrade = (key: 'damageRank' | 'drawRank' | 'healthRank' | 'eagleEyeRank' | 'barricadeRank', base: number) => {
    const cost = Math.round(base * Math.pow(1.32, save[key]));
    if (save.coins < cost || save[key] >= 12) return;
    setSave((prev) => ({ ...prev, coins: prev.coins - cost, [key]: prev[key] + 1 }));
  };

  const buyArrowPack = (type: Exclude<ArrowType, 'NORMAL'>) => {
    const info = ARROW_INFO[type];
    if (save.coins < info.cost) return;
    setSave((prev) => ({
      ...prev,
      coins: prev.coins - info.cost,
      ammo: { ...prev.ammo, [type]: prev.ammo[type] + info.pack },
    }));
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* 3-Zone Header Bar */}
      <header className="flex items-center justify-between gap-4 px-4 sm:px-6 py-3 border-b border-slate-800 bg-slate-900/90">
        <span className="font-display text-lg sm:text-xl font-bold tracking-wide text-slate-100 whitespace-nowrap shrink-0">
          Stickman Archer: Zombie Siege
        </span>

        <div className="flex items-center gap-4 text-xs sm:text-sm text-slate-300 font-mono-num whitespace-nowrap shrink-0">
          <span className="text-amber-400 font-semibold">Coins: {save.coins + (gameState === 'PLAYING' ? hud.sessionCoins : 0)}</span>
          <span aria-hidden="true" className="text-slate-600">·</span>
          <span>Stage {save.highestLevel}</span>
          <span aria-hidden="true" className="hidden sm:inline text-slate-600">·</span>
          <span className="hidden sm:inline">Best Endless: {save.bestEndless}</span>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setSave((p) => ({ ...p, sfxEnabled: !p.sfxEnabled }))}
            aria-label="Toggle sound"
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors"
          >
            {save.sfxEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
          </button>
          <button
            type="button"
            onClick={() => setActiveModal(activeModal === 'SETTINGS' ? 'NONE' : 'SETTINGS')}
            aria-label="Open settings"
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Main Arena Stage */}
      <main className="flex-1 flex flex-col items-center justify-center p-3 sm:p-5 gap-3">
        <div className="relative w-full max-w-6xl bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-2xl">
          {/* Top In-Game HUD */}
          {gameState !== 'TITLE_MENU' && (
            <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-2.5 bg-slate-900/95 border-b border-slate-800 text-xs font-mono-num">
              <div className="flex items-center gap-4">
                <div>
                  <div className="text-slate-400">Hero HP: {hud.hp}/{hud.maxHp}</div>
                  <div className="w-28 h-2 bg-slate-800 rounded overflow-hidden mt-1">
                    <div className="h-full bg-emerald-500 transition-all" style={{ width: `${(hud.hp / hud.maxHp) * 100}%` }} />
                  </div>
                </div>
                <div>
                  <div className="text-slate-400">Wall HP: {hud.wallHp}/{hud.maxWallHp}</div>
                  <div className="w-28 h-2 bg-slate-800 rounded overflow-hidden mt-1">
                    <div className="h-full bg-sky-400 transition-all" style={{ width: `${(hud.wallHp / hud.maxWallHp) * 100}%` }} />
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-4 text-slate-200">
                <span>{mode === 'TIME_ATTACK' ? `Time ${hud.timeLeft}s` : `Wave ${hud.wave}${mode === 'CAMPAIGN' ? `/${hud.totalWaves}` : ''}`}</span>
                <span aria-hidden="true" className="text-slate-600">·</span>
                <span>Wind {hud.wind > 0 ? `+${hud.wind}` : hud.wind}</span>
                <span aria-hidden="true" className="text-slate-600">·</span>
                <span>Score {hud.score}</span>
                {hud.combo >= 2 && <span className="text-amber-400 font-bold">Combo x{hud.combo}</span>}
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={repairWall}
                  disabled={save.coins < 40 || hud.wallHp >= hud.maxWallHp}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-xs font-medium whitespace-nowrap"
                >
                  <Shield className="w-3.5 h-3.5 text-sky-400" />
                  Repair (40c)
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const next = worldRef.current.state === 'PLAYING' ? 'PAUSED' : 'PLAYING';
                    worldRef.current.state = next;
                    setGameState(next);
                  }}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium whitespace-nowrap"
                >
                  {gameState === 'PAUSED' ? <Play className="w-3.5 h-3.5" /> : <Pause className="w-3.5 h-3.5" />}
                  {gameState === 'PAUSED' ? 'Resume' : 'Pause'}
                </button>
              </div>
            </div>
          )}

          {hud.bossMaxHp > 0 && gameState === 'PLAYING' && (
            <div className="px-4 py-1.5 bg-red-950/60 border-b border-red-900/50 flex items-center gap-3 text-xs font-mono-num">
              <span className="text-red-300 font-semibold whitespace-nowrap">Siege Overlord</span>
              <div className="flex-1 h-2.5 bg-slate-900 rounded overflow-hidden">
                <div className="h-full bg-red-600" style={{ width: `${Math.max(0, (hud.bossHp / hud.bossMaxHp) * 100)}%` }} />
              </div>
              <span className="text-red-200">{hud.bossHp}/{hud.bossMaxHp}</span>
            </div>
          )}

          {/* Interactive Playable Canvas */}
          <div className="relative aspect-[15/8] w-full bg-slate-950">
            <canvas
              ref={canvasRef}
              width={W}
              height={H}
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerCancel={handlePointerUp}
              className="w-full h-full block touch-none cursor-crosshair"
            />

            {/* Title Menu Overlay */}
            {gameState === 'TITLE_MENU' && (
              <div className="absolute inset-0 bg-slate-950/85 backdrop-blur-sm flex flex-col items-center justify-center p-6 text-center">
                <h1 className="font-display text-3xl sm:text-5xl font-bold tracking-tight text-white max-w-xl">
                  Stickman Archer: Zombie Siege
                </h1>
                <p className="text-slate-300 text-sm sm:text-base mt-2 max-w-lg">
                  Drag backward to draw your bow, read the wind, and land 3x headshots. Full Kotlin + LibGDX Android APK source is included in this repo.
                </p>

                <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
                  <button
                    type="button"
                    onClick={() => startMatch('CAMPAIGN', save.highestLevel)}
                    className="px-6 py-3 rounded-lg bg-red-600 hover:bg-red-500 text-white font-semibold text-sm sm:text-base transition-colors whitespace-nowrap"
                  >
                    Play Campaign (Stage {save.highestLevel})
                  </button>
                  <button
                    type="button"
                    onClick={() => startMatch('ENDLESS', 1)}
                    className="px-5 py-3 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-100 font-medium text-sm transition-colors whitespace-nowrap"
                  >
                    Endless Survival
                  </button>
                  <button
                    type="button"
                    onClick={() => startMatch('BOSS_RUSH', 1)}
                    className="px-5 py-3 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-100 font-medium text-sm transition-colors whitespace-nowrap"
                  >
                    Boss Rush
                  </button>
                  <button
                    type="button"
                    onClick={() => startMatch('TIME_ATTACK', 1)}
                    className="px-5 py-3 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-100 font-medium text-sm transition-colors whitespace-nowrap"
                  >
                    Time Attack
                  </button>
                </div>

                <div className="mt-4 flex flex-wrap items-center justify-center gap-3">
                  <button
                    type="button"
                    onClick={() => setActiveModal('UPGRADES')}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-900 border border-slate-700 hover:border-slate-500 text-sm font-medium whitespace-nowrap"
                  >
                    <ArrowUpCircle className="w-4 h-4 text-amber-400" />
                    Upgrade Tree
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveModal('SHOP')}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-900 border border-slate-700 hover:border-slate-500 text-sm font-medium whitespace-nowrap"
                  >
                    <ShoppingBag className="w-4 h-4 text-sky-400" />
                    Arrow Shop
                  </button>
                </div>
              </div>
            )}

            {/* Pause / Victory / Game Over Modal */}
            {(gameState === 'PAUSED' || gameState === 'VICTORY' || gameState === 'GAME_OVER') && (
              <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
                <div className="bg-slate-900 border border-slate-700 rounded-xl p-6 max-w-sm w-full text-center space-y-4">
                  <h2 className="font-display text-3xl font-bold text-white">
                    {gameState === 'PAUSED' ? 'Game Paused' : gameState === 'VICTORY' ? 'Stage Cleared' : 'Siege Fallen'}
                  </h2>
                  {gameState === 'VICTORY' && (
                    <div className="text-2xl text-amber-400 tracking-widest">
                      {'★'.repeat(hud.stars)}{'☆'.repeat(3 - hud.stars)}
                    </div>
                  )}
                  <div className="text-sm text-slate-300 font-mono-num">
                    Kills {hud.kills} · Score {hud.score} · Coins +{hud.sessionCoins}
                  </div>
                  <div className="flex flex-col gap-2.5 pt-2">
                    {gameState === 'PAUSED' && (
                      <button
                        type="button"
                        onClick={() => {
                          worldRef.current.state = 'PLAYING';
                          setGameState('PLAYING');
                        }}
                        className="w-full py-2.5 rounded-lg bg-red-600 hover:bg-red-500 text-white font-semibold text-sm"
                      >
                        Resume
                      </button>
                    )}
                    {gameState === 'VICTORY' && mode === 'CAMPAIGN' && (
                      <button
                        type="button"
                        onClick={() => startMatch('CAMPAIGN', level + 1)}
                        className="w-full py-2.5 rounded-lg bg-red-600 hover:bg-red-500 text-white font-semibold text-sm"
                      >
                        Next Stage
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => startMatch(mode, level)}
                      className="w-full py-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-100 font-medium text-sm inline-flex items-center justify-center gap-2"
                    >
                      <RotateCcw className="w-4 h-4" />
                      Play Again
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        worldRef.current.state = 'TITLE_MENU';
                        setGameState('TITLE_MENU');
                      }}
                      className="w-full py-2.5 rounded-lg bg-slate-800/60 hover:bg-slate-800 text-slate-300 font-medium text-sm"
                    >
                      Main Menu
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Bottom Arrow Quiver Selector */}
          {gameState === 'PLAYING' && (
            <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 bg-slate-900 border-t border-slate-800">
              <div className="flex flex-wrap items-center gap-1.5">
                {(Object.keys(ARROW_INFO) as ArrowType[]).map((t) => {
                  const qty = t === 'NORMAL' ? 'INF' : save.ammo[t];
                  const active = selectedArrow === t;
                  return (
                    <button
                      key={t}
                      type="button"
                      disabled={t !== 'NORMAL' && save.ammo[t] <= 0}
                      onClick={() => setSelectedArrow(t)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium font-mono-num transition-colors whitespace-nowrap ${
                        active ? 'bg-red-600 text-white' : 'bg-slate-800 text-slate-300 hover:bg-slate-700 disabled:opacity-40'
                      }`}
                    >
                      {ARROW_INFO[t].label} ({qty})
                    </button>
                  );
                })}
              </div>
              <span className="text-xs text-slate-400">Drag backward anywhere on arena to aim & fire</span>
            </div>
          )}
        </div>
      </main>

      {/* Upgrades / Shop / Settings Modal */}
      {activeModal !== 'NONE' && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-xl max-w-lg w-full p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-display text-2xl font-bold text-white">
                {activeModal === 'UPGRADES' ? 'Upgrade Tree' : activeModal === 'SHOP' ? 'Arrow Shop' : 'Settings'}
              </h3>
              <button
                type="button"
                onClick={() => setActiveModal('NONE')}
                className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium"
              >
                Close
              </button>
            </div>

            {activeModal === 'UPGRADES' && (
              <div className="space-y-3 text-sm">
                {[
                  { key: 'damageRank' as const, name: 'Arrow Power', desc: '+14% damage per rank', base: 90 },
                  { key: 'drawRank' as const, name: 'Quick Draw', desc: '+9% faster reload per rank', base: 85 },
                  { key: 'healthRank' as const, name: 'Fortitude', desc: '+25 Hero & +40 Wall HP', base: 80 },
                  { key: 'eagleEyeRank' as const, name: 'Eagle Eye', desc: 'Longer trajectory preview', base: 95 },
                  { key: 'barricadeRank' as const, name: 'Iron Wall', desc: '+6% barricade armor', base: 110 },
                ].map((u) => {
                  const cost = Math.round(u.base * Math.pow(1.32, save[u.key]));
                  const maxed = save[u.key] >= 12;
                  return (
                    <div key={u.key} className="flex items-center justify-between gap-4 py-2 border-b border-slate-800/70">
                      <div>
                        <div className="font-semibold text-white">{u.name} (Rank {save[u.key]}/12)</div>
                        <div className="text-xs text-slate-400">{u.desc}</div>
                      </div>
                      <button
                        type="button"
                        disabled={maxed || save.coins < cost}
                        onClick={() => buyUpgrade(u.key, u.base)}
                        className="px-3.5 py-2 rounded-lg bg-red-600 hover:bg-red-500 disabled:bg-slate-800 disabled:text-slate-500 text-xs font-semibold font-mono-num whitespace-nowrap"
                      >
                        {maxed ? 'Maxed' : `${cost} Coins`}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}

            {activeModal === 'SHOP' && (
              <div className="space-y-3 text-sm">
                {(Object.keys(ARROW_INFO) as ArrowType[])
                  .filter((t): t is Exclude<ArrowType, 'NORMAL'> => t !== 'NORMAL')
                  .map((t) => {
                    const info = ARROW_INFO[t];
                    return (
                      <div key={t} className="flex items-center justify-between gap-4 py-2 border-b border-slate-800/70">
                        <div>
                          <div className="font-semibold text-white">{info.label} Arrows (Owned: {save.ammo[t]})</div>
                          <div className="text-xs text-slate-400">Pack of {info.pack} · Damage x{info.mult}</div>
                        </div>
                        <button
                          type="button"
                          disabled={save.coins < info.cost}
                          onClick={() => buyArrowPack(t)}
                          className="px-3.5 py-2 rounded-lg bg-red-600 hover:bg-red-500 disabled:bg-slate-800 disabled:text-slate-500 text-xs font-semibold font-mono-num whitespace-nowrap"
                        >
                          Buy {info.cost}c
                        </button>
                      </div>
                    );
                  })}
              </div>
            )}

            {activeModal === 'SETTINGS' && (
              <div className="space-y-3 text-sm">
                <label className="flex items-center justify-between py-1 cursor-pointer">
                  <span>Show Dotted Trajectory Preview</span>
                  <input
                    type="checkbox"
                    checked={save.trajectoryPreview}
                    onChange={(e) => setSave((p) => ({ ...p, trajectoryPreview: e.target.checked }))}
                    className="w-4 h-4 accent-red-600"
                  />
                </label>
                <label className="flex items-center justify-between py-1 cursor-pointer">
                  <span>Left-Handed Aiming Mode</span>
                  <input
                    type="checkbox"
                    checked={save.leftHanded}
                    onChange={(e) => setSave((p) => ({ ...p, leftHanded: e.target.checked }))}
                    className="w-4 h-4 accent-red-600"
                  />
                </label>
                <label className="flex items-center justify-between py-1 cursor-pointer">
                  <span>Blood Effects (Off by default)</span>
                  <input
                    type="checkbox"
                    checked={save.bloodEnabled}
                    onChange={(e) => setSave((p) => ({ ...p, bloodEnabled: e.target.checked }))}
                    className="w-4 h-4 accent-red-600"
                  />
                </label>
                <label className="flex items-center justify-between py-1 cursor-pointer">
                  <span>Sound Effects</span>
                  <input
                    type="checkbox"
                    checked={save.sfxEnabled}
                    onChange={(e) => setSave((p) => ({ ...p, sfxEnabled: e.target.checked }))}
                    className="w-4 h-4 accent-red-600"
                  />
                </label>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
