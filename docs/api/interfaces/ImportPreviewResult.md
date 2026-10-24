[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ImportPreviewResult

# Interface: ImportPreviewResult

Defined in: [models.ts:722](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L722)

Preview summary displayed to the user prior to committing a bookmark file import.

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="format"></a> `format` | `string` | Detected file format (e.g. `"csv"`, `"json"`, `"xml"`). | [models.ts:726](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L726) |
| <a id="rows"></a> `rows` | `string`[] | Sample text lines or titles extracted from the file for user confirmation. | [models.ts:731](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L731) |
| <a id="totaldetectedcount"></a> `totalDetectedCount?` | `number` | Total number of valid bookmark entries detected in the source. | [models.ts:736](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L736) |
