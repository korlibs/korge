---
permalink: /templates/tags/
group: templates
layout: default
title: Template Tags
title_short: Tags
fa-icon: fas fa-tags
priority: 20
---

KorTE include some basic tags by default.



### Syntax

{% raw %}
In KorTE you can use blocks like this:

* `{% block_marker %}`
* `{% block %}...{% endblock %}`

And you can trim spaces on the left, right or both sides of the block:

* Left: `{%- block_marker %}`
* Right: `{% block_marker -%}`
* Both: `{%- block_marker -%}`


## FOR

### The simplest syntax for for is:

```liquid
{% for key in list %}{{ key }}{% endfor %}
```

### You can also iterate maps and lists of pairs:

```liquid
{% for key, value in map %}{{ key }}={{ value }}{% endfor %}
```

### It is possible define an else block to be executed when the iterable is empty:

```liquid
{% for item in expression %}
    {{ item }},
{% else %}
    List is empty
{% endfor %}
```

### Inside loops, there is a special variable called `loop` with information about the iteration.

```liquid
{% for item in expression %}
    {{ loop.length }}
    {{ loop.index }}
    {{ loop.index0 }}
    {{ loop.revindex }}
    {{ loop.first }} -- boolean
    {{ loop.last }} -- boolean
{% endfor %}
```

### You can iterate ranges using the `..` operator:

```liquid
{% for n in 0..9 %}{{ n }}, {% endfor %}
```

> `0, 1, 2, 3, 4, 5, 6, 7, 8, 9, `

## IF / ELSEIF / ELSE

### The basic syntax:

```liquid
{% if expression %}display only if expression is true{% endif %}
```

### IF / ELSE syntax:

```liquid
{% if expression %}
    display only if expression is true
{% else %}
    display only if expression is false
{% endif %}
```

### IF / ELSEIF / ELSE syntax:

```liquid
{% if expression1 %}
    display only if expression is true
{% elseif expression2 %}
    display if expression2 is true and expression1 was false
{% else %}
    display only if not matched other entries
{% endif %}
```

## SWITCH + CASE

```liquid
{% switch expression %}
    {% case "a" %}Case a
    {% case "b" %}Case b
    {% default %}Other cases
{% endswitch %}
```

## SET

In order to create temporal variables you can use the set tag:

```liquid
{% set variable = expression %}
```

## DEBUG

Logs message to the standard output for debugging:

```liquid
{% debug "test" %}
```

## CAPTURE

```liquid
{% capture variable %}REPEAT{% endcapture %}

{{ variable }} and {{ variable }}
```

## MACRO + IMPORT

### `_macros.html`
```liquid
{% macro sum(a, b) %}
    {{ a + b }}
{% endmacro %}
```

### `index.html`
```liquid
{% import "_macros.html" as macros %}
{{ macros.sum(1, 2) }}
```

## INCLUDE

### `_include_.html`
```liquid
HELLO {{ name }}
```

### `index.html`
```liquid
{% set name = "WORLD" %}{% include "_include.html" %}
{% set name = "NAME" %}{% include "_include.html" %}
```

## EXTENDS + BLOCK

KorTE supports template inheritance with multiply blocks.

### `_base.html`
```liquid
<html><head></head><body>
{% block content %}default content{% endblock %}
</body></html>
```

### `_two_columns.html`
```liquid
{% extends "_base.html" %}
{% block content %}
    <div>{% block left %}default left column{% endblock %}</div>
    <div>{% block right %}default right column{% endblock %}</div>
{% endblock %}
```

### `index.html`
```liquid
{% extends "_two_columns.html" %}
{% block left %}
    My left column
{% endblock %}
{% block right %}
    My prefix {{ parent() }} with additional content
{% endblock %}
```
{% endraw %}
