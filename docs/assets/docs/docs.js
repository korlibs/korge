// NOTICE!! DO NOT USE ANY OF THIS JAVASCRIPT
// IT'S ALL JUST JUNK FOR OUR DOCS!
// ++++++++++++++++++++++++++++++++++++++++++

/*!
 * JavaScript for Bootstrap's docs (https://getbootstrap.com/)
 * Copyright 2011-2023 The Bootstrap Authors
 * Licensed under the Creative Commons Attribution 3.0 Unported License.
 * For details, see https://creativecommons.org/licenses/by/3.0/.
 */

(() => {
    'use strict'

    // Scroll the active sidebar link into view
    const sidenav = document.querySelector('.bd-sidebar')
    const sidenavActiveLink = document.querySelector('.bd-links-nav .active')

    if (sidenav && sidenavActiveLink) {
        const sidenavHeight = sidenav.clientHeight
        const sidenavActiveLinkTop = sidenavActiveLink.offsetTop
        const sidenavActiveLinkHeight = sidenavActiveLink.clientHeight
        const viewportTop = sidenavActiveLinkTop
        const viewportBottom = viewportTop - sidenavHeight + sidenavActiveLinkHeight

        if (sidenav.scrollTop > viewportTop || sidenav.scrollTop < viewportBottom) {
            sidenav.scrollTop = viewportTop - (sidenavHeight / 2) + (sidenavActiveLinkHeight / 2)
        }
    }
})();


// NOTICE!! DO NOT USE ANY OF THIS JAVASCRIPT
// IT'S ALL JUST JUNK FOR OUR DOCS!
// ++++++++++++++++++++++++++++++++++++++++++

// NOTICE!!! Initially embedded in our docs this JavaScript
// file contains elements that can help you create reproducible
// use cases in StackBlitz for instance.
// In a real project please adapt this content to your needs.
// ++++++++++++++++++++++++++++++++++++++++++

/*!
 * JavaScript for Bootstrap's docs (https://getbootstrap.com/)
 * Copyright 2011-2023 The Bootstrap Authors
 * Licensed under the Creative Commons Attribution 3.0 Unported License.
 * For details, see https://creativecommons.org/licenses/by/3.0/.
 */

/* global bootstrap: false */

(() => {
    'use strict'

    // --------
    // Tooltips
    // --------
    // Instantiate all tooltips in a docs or StackBlitz
    document.querySelectorAll('[data-bs-toggle="tooltip"]')
        .forEach(tooltip => {
            new bootstrap.Tooltip(tooltip)
        })

    // --------
    // Popovers
    // --------
    // Instantiate all popovers in docs or StackBlitz
    document.querySelectorAll('[data-bs-toggle="popover"]')
        .forEach(popover => {
            new bootstrap.Popover(popover)
        })

    // -------------------------------
    // Toasts
    // -------------------------------
    // Used by 'Placement' example in docs or StackBlitz
    const toastPlacement = document.getElementById('toastPlacement')
    if (toastPlacement) {
        document.getElementById('selectToastPlacement').addEventListener('change', function () {
            if (!toastPlacement.dataset.originalClass) {
                toastPlacement.dataset.originalClass = toastPlacement.className
            }

            toastPlacement.className = `${toastPlacement.dataset.originalClass} ${this.value}`
        })
    }

    // Instantiate all toasts in docs pages only
    document.querySelectorAll('.bd-example .toast')
        .forEach(toastNode => {
            const toast = new bootstrap.Toast(toastNode, {
                autohide: false
            })

            toast.show()
        })

    // Instantiate all toasts in docs pages only
    // js-docs-start live-toast
    const toastTrigger = document.getElementById('liveToastBtn')
    const toastLiveExample = document.getElementById('liveToast')

    if (toastTrigger) {
        const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLiveExample)
        toastTrigger.addEventListener('click', () => {
            toastBootstrap.show()
        })
    }
    // js-docs-end live-toast

    // -------------------------------
    // Alerts
    // -------------------------------
    // Used in 'Show live alert' example in docs or StackBlitz

    // js-docs-start live-alert
    const alertPlaceholder = document.getElementById('liveAlertPlaceholder')
    const appendAlert = (message, type) => {
        const wrapper = document.createElement('div')
        wrapper.innerHTML = [
            `<div class="alert alert-${type} alert-dismissible" role="alert">`,
            `   <div>${message}</div>`,
            '   <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>',
            '</div>'
        ].join('')

        alertPlaceholder.append(wrapper)
    }

    const alertTrigger = document.getElementById('liveAlertBtn')
    if (alertTrigger) {
        alertTrigger.addEventListener('click', () => {
            appendAlert('Nice, you triggered this alert message!', 'success')
        })
    }
    // js-docs-end live-alert

    // --------
    // Carousels
    // --------
    // Instantiate all non-autoplaying carousels in docs or StackBlitz
    document.querySelectorAll('.carousel:not([data-bs-ride="carousel"])')
        .forEach(carousel => {
            bootstrap.Carousel.getOrCreateInstance(carousel)
        })

    // -------------------------------
    // Checks & Radios
    // -------------------------------
    // Indeterminate checkbox example in docs and StackBlitz
    document.querySelectorAll('.bd-example-indeterminate [type="checkbox"]')
        .forEach(checkbox => {
            if (checkbox.id.includes('Indeterminate')) {
                checkbox.indeterminate = true
            }
        })

    // -------------------------------
    // Links
    // -------------------------------
    // Disable empty links in docs examples only
    document.querySelectorAll('.bd-content [href="#"]')
        .forEach(link => {
            link.addEventListener('click', event => {
                event.preventDefault()
            })
        })

    // -------------------------------
    // Modal
    // -------------------------------
    // Modal 'Varying modal content' example in docs and StackBlitz
    // js-docs-start varying-modal-content
    const exampleModal = document.getElementById('exampleModal')
    if (exampleModal) {
        exampleModal.addEventListener('show.bs.modal', event => {
            // Button that triggered the modal
            const button = event.relatedTarget
            // Extract info from data-bs-* attributes
            const recipient = button.getAttribute('data-bs-whatever')
            // If necessary, you could initiate an Ajax request here
            // and then do the updating in a callback.

            // Update the modal's content.
            const modalTitle = exampleModal.querySelector('.modal-title')
            const modalBodyInput = exampleModal.querySelector('.modal-body input')

            modalTitle.textContent = `New message to ${recipient}`
            modalBodyInput.value = recipient
        })
    }
    // js-docs-end varying-modal-content

    // -------------------------------
    // Offcanvas
    // -------------------------------
    // 'Offcanvas components' example in docs only
    const myOffcanvas = document.querySelectorAll('.bd-example-offcanvas .offcanvas')
    if (myOffcanvas) {
        myOffcanvas.forEach(offcanvas => {
            offcanvas.addEventListener('show.bs.offcanvas', event => {
                event.preventDefault()
            }, false)
        })
    }
})();

(() => {
    window.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('main table').forEach((block) => {
            block.classList.add('table')
        })

        document.querySelectorAll('pre code').forEach((block) => {
            const pre = block.parentNode
            const wrapper = document.createElement('div')
            pre.className = "custom-scroll"
            wrapper.className = 'bd-code-snippet '
            wrapper.innerHTML = `<div class=\'bd-clipboard\'></div><div class="highlight">${pre.outerHTML}</div>`
            //<div className='bd-code-snippet' markdown='1'>
            //<div class='bd-clipboard'></div>
            //<div class='highlight' markdown='1'>
            pre.replaceWith(wrapper)
        })

        document.querySelectorAll('pre code').forEach((block) => {
            //console.log('pre code', block);
            hljs.highlightBlock(block);
        });


        'use strict'

        // Insert copy to clipboard button before .highlight
        const btnTitle = 'Copy to clipboard'
        const btnEdit = 'Edit on StackBlitz'

        const btnHtml = [
            '<div class="bd-code-snippet">',
            '  <div class="bd-clipboard">',
            '    <button type="button" class="btn-clipboard">',
            '      <i class="bi bi-clipboard"></i>',
            '    </button>',
            '  </div>',
            '</div>'
        ].join('')

        // Wrap programmatically code blocks and add copy btn.
        document.querySelectorAll('.highlight')
            .forEach(element => {
                // Ignore examples made by shortcode
                if (!element.closest('.bd-example-snippet')) {
                    element.insertAdjacentHTML('beforebegin', btnHtml)
                    element.previousElementSibling.append(element)
                }
            })

        /**
         *
         * @param {string} selector
         * @param {string} title
         */
        function snippetButtonTooltip(selector, title) {
            document.querySelectorAll(selector).forEach(btn => {
                bootstrap.Tooltip.getOrCreateInstance(btn, { title })
            })
        }

        snippetButtonTooltip('.btn-clipboard', btnTitle)
        snippetButtonTooltip('.btn-edit', btnEdit)

        const clipboard = new ClipboardJS('.btn-clipboard', {
            target: trigger => trigger.closest('.bd-code-snippet').querySelector('.highlight'),
            text: trigger => trigger.closest('.bd-code-snippet').querySelector('.highlight').textContent.trimEnd()
        })

        clipboard.on('success', event => {
            const iconFirstChild = event.trigger.querySelector('.bi')
            const tooltipBtn = bootstrap.Tooltip.getInstance(event.trigger)

            tooltipBtn.setContent({ '.tooltip-inner': 'Copied!' })
            event.trigger.addEventListener('hidden.bs.tooltip', () => {
                tooltipBtn.setContent({ '.tooltip-inner': btnTitle })
            }, { once: true })
            event.clearSelection()
            iconFirstChild.classList.replace('bi-clipboard', 'bi-check2')

            setTimeout(() => {
                iconFirstChild.classList.replace('bi-check2', 'bi-clipboard')
            }, 2000)
        })

        clipboard.on('error', event => {
            const modifierKey = /mac/i.test(navigator.userAgent) ? '\u2318' : 'Ctrl-'
            const fallbackMsg = `Press ${modifierKey}C to copy`
            const tooltipBtn = bootstrap.Tooltip.getInstance(event.trigger)

            tooltipBtn.setContent({ '.tooltip-inner': fallbackMsg })
            event.trigger.addEventListener('hidden.bs.tooltip', () => {
                tooltipBtn.setContent({ '.tooltip-inner': btnTitle })
            }, { once: true })
        })
    });

})();

(() => {
    $(function () {
        var navSelector = "#toc";
        var $myNav = $(navSelector);
        Toc.init({
            $nav: $myNav,
            $scope: $('main'),
        });
        $("body").scrollspy({
            target: navSelector,
        });
    });
})();

(async () => {

    const catalogJsonCacheDate = localStorage.getItem('catalogJsonCacheDate')|0

    if ((Date.now() - catalogJsonCacheDate) >= 3600 * 1000) {
        localStorage.setItem('catalogJsonCacheDate', Date.now().toString())
        const fetchResult = await fetch("https://store.korge.org/catalog.json")
        const resultStr = await fetchResult.text()
        localStorage.setItem('catalogJsonCacheContent', resultStr)
    }
    const result = JSON.parse(localStorage.getItem('catalogJsonCacheContent'))
    const linkList = []
    for (const item of result) {
        const cat = item.category
        const title = item.title
        const url = item.url
        const path = url.replace(/^https:\/\/store\.korge\.org/, '')
        //<li><a href="/templates/tags/" class=" ">Tags</a></li>
        const li = document.createElement("li")
        const a = document.createElement("a")
        a.href = `/store_proxy/?url=${path}`
        a.className = "bd-links-link d-inline-block rounded"
        a.textContent = title
        li.appendChild(a)
        linkList.push(a)
        const catUL = document.querySelector(`#section-${cat} ul`)
        if (catUL) {
            catUL.appendChild(li)
        }
    }

    const [currentLocation] = document.location.href.split('#')

    if (currentLocation.indexOf("/store_proxy") >= 0) {
        for (/** @type HTMLAnchorElement */ const link of linkList) {
            //console.log(link.href, document.location.href)
            if (link.href === currentLocation) {
                link.classList.add('active')
                //link.dataset['aria-current'] = 'page'
                link.scrollIntoView({block: "center"})
                break;
            }
        }
    }
})();
