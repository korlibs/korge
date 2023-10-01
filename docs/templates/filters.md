---
permalink: /templates/filters/
group: templates
layout: default
title: Template Filters
title_short: Filters
description: KorTE include some basic filters by default.
fa-icon: fas fa-filter
priority: 50
---

{% raw %}

## CAPITALIZE

Changes the first letter of the string to be uppercased.

```liquid
{{ "hellO"|capitalize }}
```
> `HellO`

## LOWER

Changes all the letter characters of the string to lower case.

```liquid
{{ "HELLo"|lower }}
```
> `hello`

## UPPER

Changes all the letter characters of the string to upper case.

```liquid
{{ "hellO"|upper }}
```
> `HELLO`

## JOIN

Joins elements in an array as a string, with a separator 

```liquid
{{ [1,2,3,4]|join(":") }}
```
> `1:2:3:4`

## LENGTH

Gets the number of elements in an array, or the number of characters in a string

```liquid
{{ ['a', 'b', 'c']|length }}, {{ "hi"|length }}
```
> `3, 2`

## QUOTE

Adds c-type quotes to a string

```liquid
{{ "I'm a test"|quote }}
```
> `"I\'m a test"`

## REVERSE

Reverses the characters in a string, or the elements in an array or list.

```liquid
{{ "hello"|reverse }}, {{ [1,2,3]|reverse }}
```
> `olleh, [3,2,1]`

## RAW

Taints the string to not be auto escaped.

```liquid
{{ "<test>" }}, {{ "<test>"|raw }}
```
> `&lt;test&gt;, <test>`

## SORT

Sorts all the elements in the provided array.

```liquid
{{ [10, 4, 7, 1]|sort }}
```
> `[1,4,7,10]`

## TRIM

Remove spaces, tabs and line breaks from the beginning and the end of the string. 

```liquid
{{ "   hello   "|trim }}
```
> `hello`

## MERGE

Combines two lists together.

```liquid
{{ [1, 2, 3]|merge([4, 5, 6]) }}
```
> `[1,2,3,4,5,6]`

## JSON_ENCODE

Converts an arbitrary object into a JSON string.

```liquid
{{ ["a", "b"]|json_encode }}
```
> `["a", "b"]`

## FORMAT

C-type formatting.

```liquid
{{ "hello %03d"|format(7) }}
```
> `hello 007`

## CHUNKED

Split a list into several lists of a specific size.

```liquid
{{ [1,2,3,4,5]|chunked(2) }}
```
> `[[1,2],[3,4],[5]]`

{% endraw %}
