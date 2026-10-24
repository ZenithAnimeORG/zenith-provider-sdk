[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / SkipInterval

# Interface: SkipInterval

Defined in: [models.ts:544](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L544)

Timecode interval marking intro/outro segments to skip during playback.

Used by skip timing integrations (e.g. AniSkip, AnimeSkip).

## Example

```ts
const opInterval: SkipInterval = {
  startTime: 92.5,
  endTime: 182.0,
  skipType: "op",
  episodeLength: 1420
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="endtime"></a> `endTime` | `number` | Timestamp in seconds where the skippable interval ends. | [models.ts:553](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L553) |
| <a id="episodelength"></a> `episodeLength?` | `number` \| `null` | Total length of the episode in seconds, if known. | [models.ts:563](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L563) |
| <a id="skiptype"></a> `skipType` | `string` | Interval category: `"op"` (opening theme), `"ed"` (ending theme), `"mixed-op"`, or `"recap"`. | [models.ts:558](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L558) |
| <a id="starttime"></a> `startTime` | `number` | Timestamp in seconds where the skippable interval begins. | [models.ts:548](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L548) |
