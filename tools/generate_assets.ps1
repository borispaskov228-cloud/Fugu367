# Генерирует модели, блокстейты, крафты, лут-таблицы и 16x16 текстуры в стиле ванили.
# Текстуры рисуются попиксельно: несколько оттенков, лепестки, листья, блики.
# Запуск: powershell -ExecutionPolicy Bypass -File tools\generate_assets.ps1
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\src\main\resources'
$assets = Join-Path $root 'assets\herbcraft'
$data = Join-Path $root 'data\herbcraft'

$types = @(
    @{ id='green';   hex='63B84A' },
    @{ id='golden';  hex='E8C63F' },
    @{ id='blue';    hex='4E9BE8' },
    @{ id='purple';  hex='A75AC9' },
    @{ id='rainbow'; hex='FF6FB1' }
)

$utf8 = New-Object System.Text.UTF8Encoding($false)

function Write-Text([string]$path, [string]$text) {
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $text, $utf8)
}

# --- Попиксельные примитивы ---

function Px($bmp, [int]$x, [int]$y, $c) {
    if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) { $bmp.SetPixel($x, $y, $c) }
}

function Box($bmp, [int]$x, [int]$y, [int]$w, [int]$h, $c) {
    for ($i = $x; $i -lt $x + $w; $i++) { for ($j = $y; $j -lt $y + $h; $j++) { Px $bmp $i $j $c } }
}

function Ell($bmp, [int]$cx, [int]$cy, [int]$rx, [int]$ry, $c) {
    if ($rx -le 0 -or $ry -le 0) { return }
    for ($y = $cy - $ry; $y -le $cy + $ry; $y++) {
        for ($x = $cx - $rx; $x -le $cx + $rx; $x++) {
            $dx = ($x - $cx) / $rx
            $dy = ($y - $cy) / $ry
            if ($dx * $dx + $dy * $dy -le 1.0001) { Px $bmp $x $y $c }
        }
    }
}

# Оттенок: $k > 0 — осветлить к белому, $k < 0 — затемнить к чёрному.
function Shade([string]$hex, [double]$k) {
    $r = [Convert]::ToInt32($hex.Substring(0,2),16)
    $g = [Convert]::ToInt32($hex.Substring(2,2),16)
    $b = [Convert]::ToInt32($hex.Substring(4,2),16)
    if ($k -ge 0) {
        $r = [int]($r + (255 - $r) * $k); $g = [int]($g + (255 - $g) * $k); $b = [int]($b + (255 - $b) * $k)
    } else {
        $r = [int]($r * (1 + $k)); $g = [int]($g * (1 + $k)); $b = [int]($b * (1 + $k))
    }
    return [System.Drawing.Color]::FromArgb($r, $g, $b)
}

function New-Texture([string]$path, [scriptblock]$draw) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    & $draw $bmp
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Три оттенка листа: тёмный низ, основной, светлый верх.
function Leaf($bmp, [int]$cx, [int]$cy, [int]$rx, [int]$ry, $pal) {
    Ell $bmp $cx $cy $rx $ry $pal.ld
    Ell $bmp $cx ($cy - 1) $rx $ry $pal.lm
    Ell $bmp $cx ($cy - 2) ([math]::Max(1, $rx - 1)) ([math]::Max(1, $ry - 1)) $pal.ll
}

# Бутон из трёх лепестков.
function Bud($bmp, [int]$cx, [int]$cy, $hexes, $pal) {
    $h0 = Shade $hexes[0] 0
    $l0 = Shade $hexes[0] 0.35
    $h1 = Shade $hexes[1] 0
    $h2 = Shade $hexes[2] 0
    Box $bmp ($cx - 1) ($cy - 3) 2 3 $h0
    Box $bmp ($cx - 1) ($cy - 3) 2 1 $l0
    Box $bmp ($cx - 3) ($cy - 1) 3 2 $h1
    Box $bmp ($cx) ($cy - 1) 3 2 $h2
    Box $bmp ($cx - 1) ($cy - 1) 2 2 $pal.core
    Px $bmp ($cx - 1) ($cy - 1) $pal.coreLight
}

# Полноценный цветок: пять лепестков с затенением + сердина.
function Bloom($bmp, [int]$cx, [int]$cy, $hexes, $pal) {
    $petals = @(
        [pscustomobject]@{ X = $cx;     Y = $cy - 3; RX = 2; RY = 2 },
        [pscustomobject]@{ X = $cx - 3; Y = $cy;     RX = 2; RY = 2 },
        [pscustomobject]@{ X = $cx + 3; Y = $cy;     RX = 2; RY = 2 },
        [pscustomobject]@{ X = $cx - 2; Y = $cy + 2; RX = 2; RY = 2 },
        [pscustomobject]@{ X = $cx + 2; Y = $cy + 2; RX = 2; RY = 2 }
    )
    for ($i = 0; $i -lt $petals.Count; $i++) {
        $p = $petals[$i]
        $hx = $hexes[$i % $hexes.Count]
        $m = Shade $hx 0
        $l = Shade $hx 0.35
        $d = Shade $hx -0.30
        Ell $bmp $p.X $p.Y $p.RX $p.RY $d
        Ell $bmp $p.X ($p.Y - 1) $p.RX $p.RY $m
        Ell $bmp $p.X ($p.Y - 2) ([math]::Max(1, $p.RX - 1)) ([math]::Max(1, $p.RY - 1)) $l
    }
    Box $bmp ($cx - 1) ($cy - 1) 2 2 $pal.core
    Px $bmp ($cx - 1) ($cy - 1) $pal.coreLight
}

# Растение целиком. $stage: 0-3 — стадии грядки, 4 — дикий куст.
function Plant($bmp, $pal, [int]$stage) {
    $hexes = $pal.petals
    $top = 12; $height = 4
    if ($stage -eq 1) { $top = 9; $height = 7 }
    if ($stage -eq 2) { $top = 7; $height = 9 }
    if ($stage -eq 3) { $top = 6; $height = 10 }
    if ($stage -eq 4) { $top = 5; $height = 11 }

    # Стебель: тёмная левая грань и светлая правая.
    Box $bmp 7 $top 1 ($height + 1) $pal.sd
    Box $bmp 8 $top 1 ($height + 1) $pal.sm
    Box $bmp 8 $top 1 2 $pal.sl

    if ($stage -eq 0) {
        Leaf $bmp 5 13 2 1 $pal
        Leaf $bmp 11 13 2 1 $pal
        Box $bmp 7 11 2 2 $pal.ll
        return
    }

    Leaf $bmp 4 12 3 2 $pal
    Leaf $bmp 12 11 3 2 $pal
    if ($stage -ge 2) { Leaf $bmp 4 9 2 1 $pal }
    if ($stage -ge 4) { Leaf $bmp 12 8 2 1 $pal }

    if ($stage -eq 1) { Bud $bmp 8 ($top - 1) $hexes $pal }
    elseif ($stage -eq 2) { Bud $bmp 8 ($top - 1) $hexes $pal }
    else { Bloom $bmp 8 ($top - 1) $hexes $pal }
}

foreach ($t in $types) {
    $id = $t.id
    $hex = $t.hex

    if ($id -eq 'rainbow') {
        $petals = @('FF6B6B','FFB347','FFE14D','63D16B','5B9BFF','B76BFF')
    } else {
        $petals = @($hex, $hex, $hex, $hex, $hex)
    }
    $coreHex = if ($id -eq 'golden') { '8A5A2B' } elseif ($id -eq 'rainbow') { 'FFFFFF' } else { 'F2C14E' }
    $pal = @{
        petals = $petals
        core = Shade $coreHex 0
        coreLight = Shade $coreHex 0.4
        ll = Shade $hex 0.35
        lm = Shade $hex 0
        ld = Shade $hex -0.32
        sd = [System.Drawing.Color]::FromArgb(52,108,42)
        sm = [System.Drawing.Color]::FromArgb(76,150,58)
        sl = [System.Drawing.Color]::FromArgb(110,186,82)
    }
    $paper = @{ l = [System.Drawing.Color]::FromArgb(240,228,198); m = [System.Drawing.Color]::FromArgb(220,203,168); d = [System.Drawing.Color]::FromArgb(188,168,132) }
    $glass = @{ l = [System.Drawing.Color]::FromArgb(210,226,236); m = [System.Drawing.Color]::FromArgb(148,172,188); d = [System.Drawing.Color]::FromArgb(104,128,146) }
    $metal = @{ l = [System.Drawing.Color]::FromArgb(238,242,246); m = [System.Drawing.Color]::FromArgb(202,210,219); d = [System.Drawing.Color]::FromArgb(150,160,172) }

    # --- Высушенная трава: аккуратная кучка листьев ---
    New-Texture (Join-Path $assets "textures\item\${id}_herb.png") {
        param($bmp)
        Leaf $bmp 5 9 3 2 $pal
        Leaf $bmp 11 8 3 2 $pal
        Leaf $bmp 8 12 3 2 $pal
        Box $bmp 7 13 2 2 $pal.sd
        Px $bmp 5 8 $pal.ll; Px $bmp 11 7 $pal.ll
    }

    # --- Скрутка: бумага с затенением, цветной кончик, уголёк ---
    New-Texture (Join-Path $assets "textures\item\${id}_joint.png") {
        param($bmp)
        Box $bmp 3 6 11 4 $paper.m
        Box $bmp 3 6 11 1 $paper.l
        Box $bmp 3 9 11 1 $paper.d
        Box $bmp 6 6 1 4 $paper.d
        Box $bmp 11 6 3 4 $pal.lm
        Box $bmp 11 6 3 1 $pal.ll
        Box $bmp 2 7 1 2 ([System.Drawing.Color]::FromArgb(244,140,42))
        Px $bmp 2 7 ([System.Drawing.Color]::FromArgb(255,214,90))
    }

    # --- Жидкость: стеклянный флакон с бликами и пузырьками ---
    New-Texture (Join-Path $assets "textures\item\${id}_liquid.png") {
        param($bmp)
        Box $bmp 6 1 4 2 $metal.m
        Box $bmp 6 1 4 1 $metal.l
        Box $bmp 6 3 4 2 $glass.d
        Box $bmp 4 5 8 10 $glass.m
        Box $bmp 4 5 2 10 $glass.l
        Box $bmp 11 5 1 10 $glass.d
        Box $bmp 5 8 6 6 $pal.lm
        Box $bmp 5 8 6 1 $pal.ll
        Box $bmp 5 8 1 6 $pal.ll
        Px $bmp 7 11 $pal.ll; Px $bmp 9 13 $pal.ll
        Box $bmp 10 6 1 3 ([System.Drawing.Color]::FromArgb(255,255,255))
    }

    # --- Вэйп: корпус с градацией, окошко, светодиод, кнопка ---
    New-Texture (Join-Path $assets "textures\item\${id}_ecig.png") {
        param($bmp)
        Box $bmp 6 0 4 3 $metal.l
        Box $bmp 9 1 1 2 $metal.d
        Box $bmp 5 3 6 11 $metal.m
        Box $bmp 5 3 2 11 $metal.l
        Box $bmp 10 3 1 11 $metal.d
        Box $bmp 6 5 4 5 $pal.lm
        Box $bmp 6 5 4 1 $pal.ll
        Box $bmp 6 5 1 5 $pal.ll
        Box $bmp 7 2 2 1 ([System.Drawing.Color]::FromArgb(120,235,90))
        Box $bmp 7 11 2 2 $metal.d
        Px $bmp 7 11 $metal.l
    }

    # --- Дикий куст ---
    New-Texture (Join-Path $assets "textures\block\${id}_wild.png") {
        param($bmp)
        Plant $bmp $pal 4
    }.GetNewClosure()

    # --- Четыре стадии роста ---
    for ($stage = 0; $stage -le 3; $stage++) {
        New-Texture (Join-Path $assets "textures\block\${id}_crop_${stage}.png") {
            param($bmp)
            Plant $bmp $pal $stage
        }.GetNewClosure()
    }

    # --- Модели предметов ---
    foreach ($what in @('herb','joint','liquid','ecig')) {
        $model = @{ parent='minecraft:item/generated'; textures=@{ layer0="herbcraft:item/${id}_${what}" } } | ConvertTo-Json -Depth 5 -Compress
        Write-Text (Join-Path $assets "models\item\${id}_${what}.json") $model
    }
    $wildItemModel = @{ parent='minecraft:item/generated'; textures=@{ layer0="herbcraft:block/${id}_wild" } } | ConvertTo-Json -Depth 5 -Compress
    Write-Text (Join-Path $assets "models\item\${id}_wild.json") $wildItemModel

    # --- Модели блоков ---
    $wildModel = @{ parent='minecraft:block/cross'; textures=@{ cross="herbcraft:block/${id}_wild" } } | ConvertTo-Json -Depth 5 -Compress
    Write-Text (Join-Path $assets "models\block\${id}_wild.json") $wildModel

    for ($age = 0; $age -le 7; $age++) {
        $stage = [math]::Floor($age / 2)
        $cropModel = @{ parent='minecraft:block/crop'; textures=@{ crop="herbcraft:block/${id}_crop_${stage}" } } | ConvertTo-Json -Depth 5 -Compress
        Write-Text (Join-Path $assets "models\block\${id}_crop_${age}.json") $cropModel
    }

    # --- Блокстейты ---
    $wildBs = @{ variants=@{ ''=@{ model="herbcraft:block/${id}_wild" } } } | ConvertTo-Json -Depth 6 -Compress
    Write-Text (Join-Path $assets "blockstates\${id}_wild.json") $wildBs

    $variants = @{}
    for ($age = 0; $age -le 7; $age++) {
        $variants["age=$age"] = @{ model="herbcraft:block/${id}_crop_${age}" }
    }
    $cropBs = @{ variants=$variants } | ConvertTo-Json -Depth 6 -Compress
    Write-Text (Join-Path $assets "blockstates\${id}_crop.json") $cropBs

    # --- Крафты ---
    $jointRecipe = @{
        type='minecraft:crafting_shaped'
        pattern=@('HPH')
        key=@{ H=@{ item="herbcraft:${id}_herb" }; P=@{ item='minecraft:paper' } }
        result=@{ item="herbcraft:${id}_joint"; count=1 }
    } | ConvertTo-Json -Depth 8 -Compress
    Write-Text (Join-Path $data "recipes\${id}_joint.json") $jointRecipe

    $ecigRecipe = @{
        type='minecraft:crafting_shaped'
        pattern=@('IG','RL')
        key=@{
            I=@{ item='minecraft:iron_ingot' }
            G=@{ item='minecraft:glass_pane' }
            R=@{ item='minecraft:redstone' }
            L=@{ item="herbcraft:${id}_liquid" }
        }
        result=@{ item="herbcraft:${id}_ecig"; count=1 }
    } | ConvertTo-Json -Depth 8 -Compress
    Write-Text (Join-Path $data "recipes\${id}_ecig.json") $ecigRecipe

    # --- Лут-таблицы ---
    $wildLoot = @{
        type='minecraft:block'
        pools=@(@{
            rolls=1
            entries=@(@{ type='minecraft:item'; name="herbcraft:${id}_herb" })
            conditions=@(@{ condition='minecraft:survives_explosion' })
        })
    } | ConvertTo-Json -Depth 10 -Compress
    Write-Text (Join-Path $data "loot_tables\blocks\${id}_wild.json") $wildLoot

    $cropLoot = @{
        type='minecraft:block'
        pools=@(
            @{
                rolls=1
                entries=@(@{ type='minecraft:item'; name="herbcraft:${id}_herb"; functions=@(@{ function='minecraft:set_count'; count=2 }) })
                conditions=@(@{ condition='minecraft:block_state_property'; block="herbcraft:${id}_crop"; properties=@{ age='7' } })
            },
            @{
                rolls=1
                entries=@(@{ type='minecraft:item'; name="herbcraft:${id}_herb" })
            }
        )
    } | ConvertTo-Json -Depth 12 -Compress
    Write-Text (Join-Path $data "loot_tables\blocks\${id}_crop.json") $cropLoot
}

Write-Output "Assets generated for $($types.Count) herb varieties."
