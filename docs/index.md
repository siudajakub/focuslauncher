---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: 'FocusLauncher'
  tagline: A minimal, focus-first, free and open source launcher for Android
  image: /icon.png
  actions:
    - theme: brand
      text: Get started
      link: /docs/user-guide/
---

<div class="home-intro">
  FocusLauncher is an apps-first Android launcher built to reduce distraction.
  It classifies your apps as essential or distracting, runs focus sessions that
  gate the distracting ones, and adds launch friction, daily limits, time
  awareness, focus history, and weekly insights — plus a lossless quick-capture
  notes surface. Search is deliberately local and apps-first: no web search,
  no Wikipedia, no online accounts.

  FocusLauncher is a fork of <a href="https://github.com/MM2-0/Kvaesitso" target="_blank">Kvaesitso</a>.
</div>

<script setup>
  import Footer from '.vitepress/theme/Footer.vue'
</script>
<div class="home-screenshots">
  <img src="/img/screenshot-1.png"></img>
  <img src="/img/screenshot-2.png"></img>
  <img src="/img/screenshot-3.png"></img>
  <img src="/img/screenshot-4.png"></img>
  <img src="/img/screenshot-5.png"></img>
  <img src="/img/screenshot-6.png"></img>

  <div class="credits">Wallpaper by Allec Gomes on <a href="https://unsplash.com/de/fotos/ein-grunes-blatt-das-auf-einem-gewasser-schwimmt-UcWUMqIsld8" target="_blank">Unsplash.com</a></div>
</div>
<Footer></Footer>
