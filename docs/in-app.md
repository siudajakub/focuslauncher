<script setup>
    import { useRoute } from 'vitepress'
    import { ref, onMounted } from 'vue'
    import QRCode from 'qrcode'

    import {VPButton} from 'vitepress/theme'


    const route = useRoute()
    const canvas = ref(null)

    const qrcode = ref(null)

    onMounted(async () => {
        qrcode.value = await QRCode.toDataURL(window.location.href)
    })

</script>

# Open documentation page

Scan this code to open the current documentation URL on another device. The Android manifest does
not expose a website-to-settings deep link.

<img :src="qrcode">
