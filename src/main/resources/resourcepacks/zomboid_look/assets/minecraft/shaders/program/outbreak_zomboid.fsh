#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

// Outbreak's Zomboid look: washed-out colour, a warm olive cast, softer contrast, a vignette and a little grain.
void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, 0.64);
    c = c * vec3(1.03, 1.0, 0.86) + vec3(0.012, 0.010, 0.0);
    c = (c - 0.5) * 0.93 + 0.5;
    vec2 d = (texCoord - 0.5) * vec2(1.15, 1.0);
    float vig = smoothstep(0.82, 0.28, length(d));
    c *= mix(0.74, 1.0, vig);
    float n = fract(sin(dot(floor(texCoord * InSize) + floor(Time * 977.0), vec2(12.9898, 78.233))) * 43758.5453);
    c += (n - 0.5) * 0.022;
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
