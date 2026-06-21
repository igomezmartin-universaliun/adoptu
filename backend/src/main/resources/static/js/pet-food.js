const foodData = {
    'DOG': [
        {type:'safe',title:'Safe Foods',items:[
            {name:'Chicken',detail:'Cooked, boneless',description:'Lean protein, easy to digest'},
            {name:'Rice',detail:'White or brown',description:'Good source of carbohydrates'},
            {name:'Carrots',detail:'Raw or cooked',description:'Low in calories, high in fiber'},
            {name:'Apples',detail:'Without seeds',description:'Rich in vitamins'},
            {name:'Peanut Butter',detail:'Unsalted, no xylitol',description:'Good protein source'}
        ]},
        {type:'harmful',title:'Harmful Foods',items:[
            {name:'Grapes',detail:'All varieties',description:'Can cause kidney failure'},
            {name:'Onions',detail:'All forms',description:'Damages red blood cells'},
            {name:'Chocolate',detail:'All types',description:'Toxic to dogs'},
            {name:'Garlic',detail:'All forms',description:'Causes anemia'}
        ]},
        {type:'cannot',title:'Cannot Eat',items:[
            {name:'Xylitol',detail:'Artificial sweetener',description:'Highly toxic'},
            {name:'Macadamia Nuts',detail:'All forms',description:'Causes weakness'},
            {name:'Avocado',detail:'Pit, skin, all',description:'Contains persin'},
            {name:'Alcohol',detail:'All forms',description:'Highly toxic'}
        ]}
    ],
    'CAT': [
        {type:'safe',title:'Safe Foods',items:[
            {name:'Cooked Fish',detail:'Without bones',description:'High in protein'},
            {name:'Chicken',detail:'Cooked, plain',description:'Good protein source'},
            {name:'Pumpkin',detail:'Plain, cooked',description:'Helps digestion'},
            {name:'Eggs',detail:'Cooked',description:'Complete protein'}
        ]},
        {type:'harmful',title:'Harmful Foods',items:[
            {name:'Raw Eggs',detail:'With avidin',description:'Interferes with biotin'},
            {name:'Raw Fish',detail:'Contains thiaminase',description:'Breaks down B vitamins'},
            {name:'Dog Food',detail:'Any',description:'Lacks taurine'},
            {name:'Milk',detail:'Most cats',description:'Lactose intolerance'}
        ]},
        {type:'cannot',title:'Cannot Eat',items:[
            {name:'Chocolate',detail:'All types',description:'Theobromine toxic'},
            {name:'Onions/Garlic',detail:'All forms',description:'Damages RBC'},
            {name:'Grapes/Raisins',detail:'All varieties',description:'Kidney damage'},
            {name:'Alcohol',detail:'All forms',description:'Very toxic'}
        ]}
    ],
    'BIRD': [
        {type:'safe',title:'Safe Foods',items:[
            {name:'Seeds',detail:'Variety mix',description:'Good fat source'},
            {name:'Fruits',detail:'Most varieties',description:'Vitamins and minerals'},
            {name:'Vegetables',detail:'Leafy greens',description:'Essential nutrients'},
            {name:'Pellets',detail:'Quality formulated',description:'Complete nutrition'}
        ]},
        {type:'harmful',title:'Harmful Foods',items:[
            {name:'Avocado',detail:'All parts',description:'Persin toxic'},
            {name:'Fruit Pits',detail:'Apple, peach',description:'Cyanide traces'},
            {name:'Salt',detail:'Any form',description:'Toxic to birds'},
            {name:'Caffeine',detail:'Coffee, tea',description:'Heart issues'}
        ]},
        {type:'cannot',title:'Cannot Eat',items:[
            {name:'Chocolate',detail:'All types',description:'Theobromine toxic'},
            {name:'Onions',detail:'All forms',description:'Damages RBC'},
            {name:'Garlic',detail:'All forms',description:'Toxic'},
            {name:'Mushrooms',detail:'Wild varieties',description:'Potential toxins'}
        ]}
    ],
    'FISH': [
        {type:'safe',title:'Safe Foods',items:[
            {name:'Flakes',detail:'Quality brands',description:'Complete fish food'},
            {name:'Pellets',detail:'Floating/sinking',description:'Balanced nutrition'},
            {name:'Frozen Food',detail:'Bloodworms, brine',description:'Protein source'},
            {name:'Vegetables',detail:'Blanched zucchini',description:'Fiber source'}
        ]},
        {type:'harmful',title:'Harmful Foods',items:[
            {name:'Bread',detail:'Any type',description:'No nutrition, fills up'},
            {name:'Human Food',detail:'Cooked meals',description:'Wrong nutrition'},
            {name:'Live Feed',detail:'Wild caught insects',description:'Potential parasites'}
        ]},
        {type:'cannot',title:'Cannot Eat',items:[
            {name:'Land Insect',detail:'Crawling bugs',description:'Not natural food'},
            {name:'Mammal Meat',detail:'Chicken, beef',description:'Wrong for most fish'},
            {name:'Dairy',detail:'Any',description:'Cannot digest'},
            {name:'Breadcrumbs',detail:'Many contain garlic',description:'Harmful additives'}
        ]}
    ],
    'RABBIT': [
        {type:'safe',title:'Safe Foods',items:[
            {name:'Hay',detail:'Timothy grass',description:'80% of diet'},
            {name:'Leafy Greens',detail:'Romaine, cilantro',description:'Daily vitamins'},
            {name:'Pellets',detail:'Timothy-based',description:'Balanced nutrition'},
            {name:'Carrots',detail:'In moderation',description:'Treat'}
        ]},
        {type:'harmful',title:'Harmful Foods',items:[
            {name:'Iceberg Lettuce',detail:'No nutrition',description:'Can cause GI stasis'},
            {name:'Beans',detail:'All legumes',description:'Gas, digestive issues'},
            {name:'Corn',detail:'Any form',description:'Cannot digest'},
            {name:'Potatoes',detail:'Any form',description:'Wrong nutrients'}
        ]},
        {type:'cannot',title:'Cannot Eat',items:[
            {name:'Chocolate',detail:'All types',description:'Theobromine toxic'},
            {name:'Onions',detail:'All forms',description:'Very toxic'},
            {name:'Garlic',detail:'All forms',description:'Toxic'},
            {name:'Avocado',detail:'All parts',description:'Contains persin'}
        ]}
    ]
};

function showFoodInfo(petType) {
    const data = foodData[petType];
    const container = document.getElementById('food-info');
    if (!data || !container) return;
    let html = '';
    data.forEach(function(cat) {
        html += '<div class="food-category"><h3>' + cat.title + '</h3><ul class="food-list">';
        cat.items.forEach(function(i) {
            html += '<li><strong>' + i.name + '</strong>';
            if (i.detail) html += ' <span class="food-detail">(' + i.detail + ')</span>';
            if (i.description) html += '<p class="food-desc">' + i.description + '</p>';
            html += '</li>';
        });
        html += '</ul></div>';
    });
    container.innerHTML = html;
}

document.querySelectorAll('.pet-type-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.pet-type-btn').forEach(function(b) { b.classList.remove('active'); });
        this.classList.add('active');
        showFoodInfo(this.dataset.type);
    });
});

showFoodInfo('DOG');