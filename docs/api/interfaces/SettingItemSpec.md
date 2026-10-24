[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / SettingItemSpec

# Interface: SettingItemSpec

Defined in: [models.ts:777](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L777)

Declarative UI setting item schema rendered natively by Zenith Material 3 Settings UI.

## Example

```json
{
  "key": "mirror",
  "title": "API Mirror",
  "description": "Select the active mirror server",
  "type": "CHOICE",
  "defaultValue": "https://api.example.com",
  "options": [
    { "value": "https://api.example.com", "label": "Main Server" },
    { "value": "https://backup.example.com", "label": "Backup Server" }
  ]
}
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="defaultvalue"></a> `defaultValue?` | `any` | Default fallback value if user has not customized this setting. | [models.ts:805](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L805) |
| <a id="description"></a> `description` | `string` | Subtitle explaining what this setting controls. | [models.ts:791](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L791) |
| <a id="issecret"></a> `isSecret?` | `boolean` | If `true`, the text value is masked as a password / API token in the UI. | [models.ts:815](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L815) |
| <a id="key"></a> `key` | `string` | Unique preference key accessed via `context.getSetting(key)`. | [models.ts:781](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L781) |
| <a id="options"></a> `options?` | `object`[] | Selectable options for `"CHOICE"` or `"MIRROR_LIST"` setting types. | [models.ts:810](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L810) |
| <a id="required"></a> `required?` | `boolean` | If `true`, the user cannot leave this setting empty. | [models.ts:820](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L820) |
| <a id="title"></a> `title` | `string` | Human-readable label displayed in settings UI. | [models.ts:786](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L786) |
| <a id="type"></a> `type` | `"TOGGLE"` \| `"TEXT"` \| `"CHOICE"` \| `"MIRROR_LIST"` | Setting input control type: - `"TOGGLE"`: Material 3 Switch toggle (boolean). - `"TEXT"`: Text input dialog / field. - `"CHOICE"`: Single-choice selection dialog from `options`. - `"MIRROR_LIST"`: Interactive server mirror selector. | [models.ts:800](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L800) |
