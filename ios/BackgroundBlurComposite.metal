#include <metal_stdlib>
using namespace metal;

kernel void nv12ToBgra(
    texture2d<float, access::read> yTexture [[texture(0)]],
    texture2d<float, access::read> cbcrTexture [[texture(1)]],
    texture2d<float, access::write> outTexture [[texture(2)]],
    uint2 gid [[thread_position_in_grid]])
{
    if (gid.x >= outTexture.get_width() || gid.y >= outTexture.get_height()) return;

    float y = yTexture.read(gid).r;
    float2 cbcr = cbcrTexture.read(gid / 2).rg;

    float cb = cbcr.x - 0.5;
    float cr = cbcr.y - 0.5;

    float r = y + 1.402 * cr;
    float g = y - 0.344136 * cb - 0.714136 * cr;
    float b = y + 1.772 * cb;

    outTexture.write(saturate(float4(r, g, b, 1.0)), gid);
}

kernel void compositeKernel(
    texture2d<float, access::read> originalTexture [[texture(0)]],
    texture2d<float, access::read> blurredTexture [[texture(1)]],
    texture2d<float, access::sample> maskTexture [[texture(2)]],
    texture2d<float, access::write> outputTexture [[texture(3)]],
    sampler maskSampler [[sampler(0)]],
    uint2 gid [[thread_position_in_grid]])
{
    if (gid.x >= outputTexture.get_width() || gid.y >= outputTexture.get_height()) return;

    float4 original = originalTexture.read(gid);
    float4 blurred = blurredTexture.read(gid);

    float2 uv = float2(gid) / float2(outputTexture.get_width(), outputTexture.get_height());
    float mask = maskTexture.sample(maskSampler, uv).r;
    mask = smoothstep(0.05, 0.95, mask);

    float4 result = mix(blurred, original, mask);
    result.a = 1.0;
    outputTexture.write(result, gid);
}
