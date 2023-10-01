---
permalink: /about/team/
group: about
layout: default
title: Team
title_short: Team
priority: 2
devs:
- {name: "Carlos Ballesteros", id: "soywiz"}
contributors:
- {name: "Carlos Ballesteros", id: "soywiz"}
---

## Team

<div class="list-group mb-3">
    {% for dev in page.devs %}
    <a class="list-group-item list-group-item-action d-flex align-items-center" href="https://github.com/{{ dev.id }}">
      <img src="https://github.com/{{ dev.id }}.png" alt="@{{ dev.id }}" width="32" height="32" class="rounded me-2" loading="lazy">
      <span>
        <strong>{{ dev.name }}</strong> @{{ dev.id }}
      </span>
    </a>
    {% endfor %}
</div>

Please make a PR adding you if you have contributed to the project.

## Contributors

TO-DO: generate automatically

<div class="list-group mb-3">
    {% for dev in page.contributors %}
    <a class="list-group-item list-group-item-action d-flex align-items-center" href="https://github.com/{{ dev.id }}">
      <img src="https://github.com/{{ dev.id }}.png" alt="@{{ dev.id }}" width="32" height="32" class="rounded me-2" loading="lazy">
      <span>
        <strong>{{ dev.name }}</strong> @{{ dev.id }}
      </span>
    </a>
    {% endfor %}
</div>

