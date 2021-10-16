# 8장 ItemProcessor
- ItemProcessor 는 스프링 배치내에서 입력 데이터를 이용해 어떤 작업을 수행하는 컴포넌트이다.
- ItemProcessor 의 일반적인 용도 중 하나는, ItemReader 가 읽은 아이템을 ItemWriter 가 쓰기 처리를 하지 않도록 필터링 하는 것이다.

## ItemProcessor
- 대부분의 시나리오 에서는 읽은 데이터를 사용해 **특정 작업을 수행** 해야 한다.
- 스프링 배치는 읽기, 처리, 쓰기 간에 고려해야 하는 문제를 잘 구분할 수 있도록 스텝을 여러 부분으로 분리 하였다.
- 이렇게 분리함으로 인해 몇 가지 고유한 작업 수행이 가능해진다.
  1. 입력의 유효성 검증
     - 이전 버전의 스프링배치에서는 **ValidatingItemReader**  를 서브클래싱 하는 방식을 사용했다.
       - ValidatingItemReader 또한 DelegatingItemReader 로 대체되었으나 제거됨
       - https://github.com/spring-projects/spring-batch/issues/3168
     - 이 방법의 가장 큰 문제는 스프링 배치가 제공하는 ItemReader 들 중 어떤 것도 ValidatingItemReader 를 서브클래싱 하고 있지 않기 때문에, 유효성 검사가 필요할 때 바로 사용가능한 ItemReader 가 없었다.
     - ItemProcessor 가 유효성 검증을 수행하면, 입력 방법에 종속되지 않고 Write 수행 전 유효성 검증을 수행할 수 있으며 **역할 과 책임** 의 관점에서 훨씬 더 의미가 있다.
  2. 기존 서비스의 재사용
     - 입력데이터를 다룰때 기존 서비스를 재사용하는 방법과 관련된 **ItemReaderAdapter** 처럼 같은 관점에서 **ItemProcessorAdapter** 를 제공한다.
  3. 스크립트 실행
     - ItemProcessor 를 사용하면 다른 개발자나 팀의 로직과 연결할 좋은 기회일 수 있지만, 다른 팀이 스프링을 사용하지 않을 수 있다.
     - **ScriptItemProcessor** 를 이용하면 특정 스크립트를 실행할 수 있는데, 스크립트에 입력으로 아이템을 제공하고 스크립트의 출력을 반환값으로 사용할 수 있다.
  4. ItemProcessor 체이닝
     - 동일한 트랜잭션 내에서 단일 아이템으로 여러 작업를 수행해야할 상황이 존재할 수 있다.
     - 단일 클래스내에서 모든 로직이 수행되도록 ItemProcessor 를 구성할 수도 있지만 이는 **단일책임의 원칙 관점** 에서 봤을때 좋은 구조가 아니다.
       - 또한 재사용성도 떨어진다.
     - 각 Processor 별로 ItemProcessor 들을 구성하고, 이를 하나로 묶어 **각 아이템에 대해 순서대로 실행될 ItemProcessor 목록** 을 만들 수 있다.

`ItemProcessor`

```java
public interface ItemProcessor<I, O> {

	/**
	 * Process the provided item, returning a potentially modified or new item for continued
	 * processing.  If the returned result is {@code null}, it is assumed that processing of the item
	 * should not continue.
	 * 
	 * A {@code null} item will never reach this method because the only possible sources are:
	 * <ul>
	 *     <li>an {@link ItemReader} (which indicates no more items)</li>
	 *     <li>a previous {@link ItemProcessor} in a composite processor (which indicates a filtered item)</li>
	 * </ul>
	 * 
	 * @param item to be processed, never {@code null}.
	 * @return potentially modified or new item for continued processing, {@code null} if processing of the
	 *  provided item should not continue.
	 * @throws Exception thrown if exception occurs during processing.
	 */
	@Nullable
	O process(@NonNull I item) throws Exception;
}
```
- ItemProcessor 는 입력값과 반환값이 반드시 동일하지 않아도 된다.
- 최종적으로 **ItemProcessor 가 반환하는 값은 ItemWriter 의 입력값** 이 된다.
- ItemProcessor 가 **null 을 반환하면 해당 아이템 이후 모든 처리가 중지** 된다.
  - 추가적으로 수행해야할 ItemProcessor/ItemWriter 는 호출되지 않는다.
  - ItemReader 의 경우 null 을 반환하면 **해당 스탭이 완료된 것으로 간주** 한다.
  - 이와 달리 ItemProcessor 는 **해당 아이템의 처리만 중지** 되고, 다른 아이템의 처리는 계속 이루어진다.

## ValidatingItemProcessor
- 스프링 배치의 이전버전에서는 유효성 검사를 위해 ValidatingItemReader 또는 DelegatingItemReader 사용했지만, ValidatingItemProcessor 를 사용하도록 대체되었다.
- ItemReader 에서 유효성 검증을 수행할 수도 있지만, 아이템이 구성된 이후 수행하는 비즈니스 규칙에 대한 유효성 검증은 리더가 아닌 다른곳에서 수행하는것이 좋다.
- 스프링 배치는 **ValidatingItemProcessor** 라는 입력 데이터 유효성 검증에 사용가능한 ItemProcessor 구현체를 제공한다.

`ValidatingItemProcessor`

```java
public class ValidatingItemProcessor<T> implements ItemProcessor<T, T>, InitializingBean {

	private Validator<? super T> validator;

	private boolean filter = false;

	/**
	 * Default constructor
	 */
	public ValidatingItemProcessor() {
	}

	/**
	 * Creates a ValidatingItemProcessor based on the given Validator.
	 *
	 * @param validator the {@link Validator} instance to be used.
	 */
	public ValidatingItemProcessor(Validator<? super T> validator) {
		this.validator = validator;
	}

	/**
	 * Set the validator used to validate each item.
	 * 
	 * @param validator the {@link Validator} instance to be used.
	 */
	public void setValidator(Validator<? super T> validator) {
		this.validator = validator;
	}

	/**
	 * Should the processor filter invalid records instead of skipping them?
	 * 
	 * @param filter if set to {@code true}, items that fail validation are filtered
	 * ({@code null} is returned).  Otherwise, a {@link ValidationException} will be
	 * thrown.
	 */
	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	/**
	 * Validate the item and return it unmodified
	 * 
	 * @return the input item
	 * @throws ValidationException if validation fails
	 */
    @Nullable
	@Override
	public T process(T item) throws ValidationException {
		try {
			validator.validate(item);
		}
		catch (ValidationException e) {
			if (filter) {
				return null; // filter the item
			}
			else {
				throw e; // skip the item
			}
		}
		return item;
	}

    @Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(validator, "Validator must not be null.");
	}

}
```
- ValidatingItemProcessor 는 입력 아이템의 유효성 검증을 수행하는 **스프링 배치의 Validator 인터페이스** 구현체를 사용할 수 있다.
- 유효성 검증에 실패하면 org.springframework.batch.item.validator.ValidationException 예외가 발생한다.

`Validator`

```java
public interface Validator<T> {
	/**
	 * Method used to validate if the value is valid.
	 * 
	 * @param value object to be validated
	 * @throws ValidationException if value is not valid.
	 */
	void validate(T value) throws ValidationException;
}
```

### 입력 유효성 검증

`의존성 추가`
- JSR-303 구현체를 사용하기 위한 의존성 추가

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

`도메인 클래스`

```java
@Data
@NoArgsConstructor
public class Customer {

    @NotNull(message = "First name is required")
    @Pattern(regexp = "[a-zA-z]", message = "First name must be alphabetical")
    private String firstName;

    @Size(min = 1, max = 1)
    @Pattern(regexp = "[a-zA-z]", message = "Middle initial must be alphabetical")
    private String middleInitial;

    @NotNull(message = "Last name is required")
    @Pattern(regexp = "[a-zA-z]", message = "Last name must be alphabetical")
    private String lastName;

    @NotNull(message = "Address is required")
    @Pattern(regexp = "[a-zA-z\\. ]+")
    private String address;

    @NotNull(message = "City is required")
    @Pattern(regexp = "[a-zA-z\\. ]+")
    private String city;

    @NotNull(message = "State is required")
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}")
    private String state;

    /**
     * Size 와 Pattern 모두 적용한 이유 ? -> Pattern 만적용해도 요구사항은 만족할테지만, 각 애너테이션을 통해 고유 메세지 지정이 가능하다. -> 또한
     * 필드 값의 길이가 잘못되었는지, 형식이 잘못되었는지 식별이 가능하다는 장점이 있다.
     */
    @NotNull(message = "Zip is required")
    @Size(min = 5, max = 5)
    @Pattern(regexp = "\\d{5}")
    private String zip;
}
```

`BeanValidatingItemProcessor`

```java
public class BeanValidatingItemProcessor<T> extends ValidatingItemProcessor<T> {
    private Validator validator;

    public BeanValidatingItemProcessor() {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.afterPropertiesSet();
        this.validator = localValidatorFactoryBean.getValidator();
    }

    public BeanValidatingItemProcessor(LocalValidatorFactoryBean localValidatorFactoryBean) {
        Assert.notNull(localValidatorFactoryBean, "localValidatorFactoryBean must not be null");
        this.validator = localValidatorFactoryBean.getValidator();
    }

    public void afterPropertiesSet() throws Exception {
        SpringValidatorAdapter springValidatorAdapter = new SpringValidatorAdapter(this.validator);
        SpringValidator<T> springValidator = new SpringValidator();
        springValidator.setValidator(springValidatorAdapter);
        springValidator.afterPropertiesSet();
        this.setValidator(springValidator);
        super.afterPropertiesSet();
    }
}
```
- JSR-303 애노테이션을 사용한 아이템은, **BeanValidatingItemProcessor** 을 사용해 검증을 수행할 수 있다.
- 이는 ValidatingItemProcessor 를 상속한 ItemProcessor 이다.

> 스프링 배치가 제공하는 Validator 인터페이스는 스프링 이 제공하는 Validator 인터페이스와 다르다. <br/>
> 스프링 배치는 이를 위한 **SpringValidator** 라는 어댑터 클래스를 제공한다.

`SpringValidaor`

```java
/**
 Adapts the org.springframework.validation.Validator interface to Validator.
 Author: Tomas Slanina, Robert Kasanicky
 */
public class SpringValidator<T> implements Validator<T>, InitializingBean {

	private org.springframework.validation.Validator validator;

	/**
	 * @see Validator#validate(Object)
	 */
    @Override
	public void validate(T item) throws ValidationException {

		if (!validator.supports(item.getClass())) {
			throw new ValidationException("Validation failed for " + item + ": " + item.getClass().getName()
					+ " class is not supported by validator.");
		}

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(item, "item");

		validator.validate(item, errors);

		if (errors.hasErrors()) {
			throw new ValidationException("Validation failed for " + item + ": " + errorsToString(errors), new BindException(errors));
		}
	}

	/**
	 * @return string of field errors followed by global errors.
	 */
	private String errorsToString(Errors errors) {
		StringBuilder builder = new StringBuilder();

		appendCollection(errors.getFieldErrors(), builder);
		appendCollection(errors.getGlobalErrors(), builder);

		return builder.toString();
	}

	/**
	 * Append the string representation of elements of the collection (separated
	 * by new lines) to the given StringBuilder.
	 */
	private void appendCollection(Collection<?> collection, StringBuilder builder) {
		for (Object value : collection) {
			builder.append("\n");
			builder.append(value.toString());
		}
	}

	public void setValidator(org.springframework.validation.Validator validator) {
		this.validator = validator;
	}

    @Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(validator, "validator must be set");

	}
}
```

`customer.csv`

```java
Richard,N,Darrow,5570 Isabella Ave,St. Louis,IL,58540
Barack,G,Donnelly,7844 S. Greenwood Ave,Houston,CA,38635
Ann,Z,Benes,2447 S. Greenwood Ave,Las Vegas,NY,55366
Laura,9S,Minella,8177 4th Street,Dallas,FL,04119
Erica,Z,Gates,3141 Farnam Street,Omaha,CA,57640
Warren,L,Darrow,4686 Mt. Lee Drive,St. Louis,NY,94935
Warren,M,Williams,6670 S. Greenwood Ave,Hollywood,FL,37288
Harry,T,Smith,3273 Isabella Ave,Houston,FL,97261
Steve,O,James,8407 Infinite Loop Drive,Las Vegas,WA,90520
Erica,Z,Neuberger,513 S. Greenwood Ave,Miami,IL,12778
Aimee,C,Hoover,7341 Vel Avenue,Mobile,AL,35928
Jonas,U,Gilbert,8852 In St.,Saint Paul,MN,57321
Regan,M,Darrow,4851 Nec Av.,Gulfport,MS,33193
Stuart,K,Mckenzie,5529 Orci Av.,Nampa,ID,18562
Sydnee,N,Robinson,894 Ornare. Ave,Olathe,KS,25606
```

`배치 잡 설정`

```java
@EnableBatchProcessing
@SpringBootApplication
public class ValidationJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public FlatFileItemReader<Customer> customerItemReader(
        @Value("#{jobParameters['customerFile']}") Resource resource
    ) {
        return new FlatFileItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .delimited()
            .names(
                "firstName", "middleInitial", "lastName",
                "address", "city", "state", "zip"
            )
            .targetType(Customer.class)
            .resource(resource)
            .build();
    }

    @Bean
    public ItemWriter<Customer> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @Bean
    public BeanValidatingItemProcessor<Customer> customerValidatingItemProcessor() {
        return new BeanValidatingItemProcessor<>();
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(5)
            .reader(customerItemReader(null))
            .processor(customerValidatingItemProcessor())
            .writer(itemWriter())
            .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("customerValidationJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ValidationJob.class, "customerFile=classpath:input/customer.csv",
            "id=2");
    }
}
```