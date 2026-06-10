const images = [
    "/images/admin-bg-opt.png",
    "/images/hero-1-opt.png",
    "/images/hero-2-opt.png",
    "/images/hero-4-opt.png"
    
];

let index = 0;
const sliderImage = document.getElementById("sliderImage");

/* Safety check */
if (sliderImage) {

    setInterval(() => {

        /* Fade out */
        sliderImage.style.opacity = 0;

        setTimeout(() => {

            /* Next image */
            index = (index + 1) % images.length;
            sliderImage.src = images[index];

            /* Fade in */
            sliderImage.style.opacity = 1;

        }, 1000);   // Fade duration

    }, 7000);       // Image visible time
}
