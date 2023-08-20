#version 120 //версия opengl для шейдера, это 1.2 вроде как

//uniform - значения которые мы можем передать из кода в шейдер

//Картинки, передаются как int
//Например: Minecraft.getInstance().textureManager.getTexture("modid:path.png".rl).getId()
uniform sampler2D sampler1;
uniform sampler2D sampler2;

uniform vec2 texelSize; //Конкретно тут это вектор (1f / width, 1f / height)
uniform vec2 direction; //направление, опишу чуть ниже
uniform float radius; //радиус (насколько далеко будут смешиваться пиксели от текущего)

//какая-то поебота из линейной алгебры 1 курса
//хотел бы я этого не понимать и принимать как должное
uniform float kernel[64];

//код из этой функции будет выполняться для каждого пикселя отдельно
void main() {
    vec2 uv = gl_TexCoord[0].st; //получаем текущие координаты, которые обрабатывает шейдер (от 0 до 1, считай проценты)
    uv.y = 1.0f - uv.y; //по сути меняем у на противоположенный, т.е. переворачиваем картинку

    float alpha = texture2D(sampler2, uv).a; //получаем цвет пикселя на `uv` текстуры `sampler2` командой texture2D(sampler2, uv) и берём `a` (прозрачность) - rgbA
    if (direction.x == 0.0 && alpha == 0.0) { //если направление по x и прозрачность равны нулю
        discard; //ОТМЕНЯЕМ вызов шейдер, ЭТО ЗНАЧИТ, что в том месте пискель НЕ БУДЕТ изменён вообще
        //например если ты такую штуку на мобе сделаешь, то в том месте ты сможешь смотреть сквозь него полностью (обратной части видно не будет)
    }

    vec4 pixel_color = texture2D(sampler1, uv) * kernel[0]; //получаем цвет текущего пикселя
    for (float f = 1; f <= radius; f++) { //от 1 до радиуса (насколько далеко будет размытие)
        vec2 offset = f * texelSize * direction; //находим текущее смещение умножая одно на другое

        //прибавляем к цвету ТЕКУЩЕГО пикселя цвет СОСЕДНИХ (и я так понимаю kernel их сглаживает)
        pixel_color += texture2D(sampler1, uv - offset) * kernel[int(f)];

        //выше был минус, тут плюс, считай просто там брались пиксели слева и внизу от этого, а тут справа и сверху от текущего
        pixel_color += texture2D(sampler1, uv + offset) * kernel[int(f)];
    }

    //итоговый цвет обрабатываемого пикселя
    gl_FragColor = vec4(pixel_color.rgb, direction.x == 0.0 ? alpha : 1.0);
}