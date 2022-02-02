#### JSONSchemaTypes
- string
- number
- object
- array
- boolean
- null

**Accept string and numbers**
```javascript
{
    "type": ["number", "string"]
}
```
#### Builtin meta data
- title
- description
- default

#### Enum

The enum keyword is used to restrict a value to a fixed set of values. It must be an array with at least one element,
where each element is unique.

```javascript
{
    "type": "string",
    "enum": ["red", "amber", "green"]
}
```

also without type

```javascript
{
"enum": ["red", "amber", "green", null, 42]
}
```

```javascript
```

#### Combining schemas

In the following schema, the anyOf keyword is used to say that the given value may be valid against
any of the given subschemas:

```javascript
{
"anyOf": [
    { "type": "string", "maxLength": 5 },
    { "type": "number", "minimum": 0 }
]
}
```

- allOf
- anyOf
- oneOf

These keywords must be set to an array, where each item is a schema.

#### Regex

- pattern

- `A` single unicode character (other than the special characters below) matches itself.
- `^`: Matches only at the beginning of the string.
- `$`: Matches only at the end of the string.
- `(...)`: Group a series of regular expressions into a single regular expression.
- `|`: Matches either the regular expression preceding or following the | symbol.
- `[abc]`: Matches any of the characters inside the square brackets.
- `[a-z]`: Matches the range of characters.
- `[^abc]`: Matches any character not listed.
- `[^a-z]`: Matches any character outside of the range.
- `+`: Matches one or more repetitions of the preceding regular expression.
- `*`: Matches zero or more repetitions of the preceding regular expression.
- `?`: Matches zero or one repetitions of the preceding regular expression.

• +?, *?, ??: The *, +, and ? qualifiers are all greedy; they match as much text as possible. Sometimes this
behavior isn’t desired and you want to match as few characters as possible.
- `{x}`: Match exactly x occurrences of the preceding regular expression.
- `{x,y}`: Match at least x and at most y occurrences of the preceding regular expression.
- `{x,}`: Match x occurrences or more of the preceding regular expression.
- `{x}?, {x,y}?, {x,}?`: Lazy versions of the above expressions

```javascript
```


#### Examples

```javascript
{
    "type": "object",
    "properties": {
        "street_address": { "type": "string" },
        "city": { "type": "string" },
        "state": { "type": "string" }
    },
    "required": ["street_address", "city", "state"]
}
```

Since we are going to reuse this schema, it is customary (but not required) to put it in the parent schema under a
key called definitions:

```javascript
{
    "definitions": {
        "address": {
            "type": "object",
            "properties": {
                "street_address": { "type": "string" },
                "city": { "type": "string" },
                "state": { "type": "string" }
            },
            "required": ["street_address", "city", "state"]
        }
    }
}
```

We can then refer to this schema snippet from elsewhere using the $ref keyword. The easiest way to describe
$ref is that it gets logically replaced with the thing that it points to. So, to refer to the above, we would include:

```javascript
{ "$ref": "#/definitions/address" }
```

The value of $ref is a string in a format called JSON Pointer

__Note: JSON Pointer aims to serve the same purpose as XPath from the XML world, but it is much simpler.__

The pound symbol `#` refers to the current document, and then the slash `/` separated keys thereafter just traverse
the keys in the objects in the document. Therefore, in our example `#/definitions/address` means:

1. go to the root of the document
2. find the value of the key "definitions"
3. within that object, find the value of the key "address"

$ref can also be a relative or absolute URI, so if you prefer to include your definitions in separate files, you can also
do that. For example:

```javascript
{ "$ref": "definitions.json#/address" }
```

1. Customer

```javascript
{
    "$schema": "http://json-schema.org/draft-04/schema#",
    "definitions": {
        "address": {
            "type": "object",
            "properties": {
                "street_address": { "type": "string" },
                "city": { "type": "string" },
                "state": { "type": "string" }
            },
            "required": ["street_address", "city", "state"]
        }
    },
    "type": "object",
    "properties": {
        "billing_address": { "$ref": "#/definitions/address" },
        "shipping_address": { "$ref": "#/definitions/address" }
    }
}
```

validates successful for:

```javascript
{
    "shipping_address": {
        "street_address": "1600 Pennsylvania Avenue NW",
        "city": "Washington",
        "state": "DC"
    },
    "billing_address": {
        "street_address": "1st Street SE",
        "city": "Washington",
        "state": "DC"
    }
}
```

#### ID

The id property serves two purposes:

- It declares a unique identifier for the schema.
- It declares a base URL against which $ref URLs are resolved.

It is best practice that id is a URL, preferably in a domain that you control. For example, if you own the foo.bar
domain, and you had a schema for addresses, you may set its id as follows:

`"id": "http://foo.bar/schemas/address.json"`

This provides a unique identifier for the schema, as well as, in most cases, indicating where it may be downloaded.

But be aware of the second purpose of the id property: that it declares a base URL for relative $ref URLs elsewhere
in the file. For example, if you had:

`{ "$ref": "person.json" }`

in the same file, a JSON schema validation library would fetch person.json from `http://foo.bar/schemas/person.json`, even if `address.json` was loaded from the local filesystem.



