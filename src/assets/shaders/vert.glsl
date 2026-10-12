#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec3 aColor;
layout (location = 3) in vec2 aUV;
layout (location = 4) in vec4 aWeights;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uLightSpace;
uniform mat4 uModel;

out vec3 vNormal;
out vec3 vColor;
out vec2 vUV;
out vec4 vWeights;
out vec3 vWorldPos;
out vec4 vLightSpacePos;

void main() {
    vec4 world = uModel * vec4(aPos, 1.0);
    gl_Position    = uProjection * uView * world;
    vNormal        = mat3(uModel) * aNormal;
    vColor         = aColor;
    vUV            = aUV;
    vWeights       = aWeights;
    vWorldPos      = world.xyz;
    vLightSpacePos = uLightSpace * world;
}