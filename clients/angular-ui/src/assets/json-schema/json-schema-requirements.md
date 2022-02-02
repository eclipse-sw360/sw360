Validators
    - required
    - pattern

input string required
input string
input string array
dropdown select one (oneOf?)

select one (sw360:users), e.g. component componentOwner
select many (sw360:users), e.g. component moderators

array of n elements
key (anyOf), value string, e.g. additionalRoles
[committer,contributor,expert]: string

[key, value]: [string: string], e.g. externalIds (release)

FORM TYPE DEFINITIONS:
//////////////////////

input
textarea
link:user
select one/many (derived from oneOf, anyOf, type string || array)
    in case of array get items type


if (oneOf) && type !array --> select one
if (array) && oneOf || anyOf --> select multiple

:: Creating new resources, their formType is defined by their resourceType
attachment (create new of them...)
release (create new of them...)

The other use case is to link to existing entities


// CHECK
////////

Always check property type
Always check if there is an item that has a type
    If there is no type, but a $ref, resolve the $ref
    In this case the client.formtype could be a known resourceType -> link to that thing
