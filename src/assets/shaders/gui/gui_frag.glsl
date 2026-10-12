#version 330 core
in vec2 vUV;
in vec4 vColor;
in float vUseTex;

uniform sampler2D uTexture;

out vec4 FragColor;

void main() {
    vec4 tex = texture(uTexture, vUV);
    // Blend về trắng khi useTex = 0 (rect solid)
    tex = mix(vec4(1.0), tex, vUseTex);
    FragColor = tex * vColor;
}