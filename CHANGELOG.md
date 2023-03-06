*In compliance with the [APACHE-2.0](https://opensource.org/licenses/Apache-2.0) license: I declare that this version of the program contains my modifications, which can be seen through the usual "git" mechanism.*  


2022-08  
Contributor(s):  
Sergii Karpenko  
>removed CustomizableWebClientBuildershould fix https://github.com/PlaytikaOSS/feign-reactive/issues/469  
>removed parameters encoding in interceptorshould fix https://github.com/PlaytikaOSS/feign-reactive/issues/455  
>Tune message in exception  
>Add validation of RepsonseEntity parameter  
>introduced ReactiveErrorMappershould fix https://github.com/PlaytikaOSS/feign-reactive/issues/478  
>add httpClient config parameterfixes https://github.com/PlaytikaOSS/feign-reactive/issues/480  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2022-05  
Contributor(s):  
Alexey Vinogradov  
>feat(#470): Added more context in the `ReactiveFeignException`Now it is possible to view the full request representation in the exception message. Not all fields have a decent `Object#toString` representation, but it clarifies what the request was. I think that this is very useful in problem-solving.  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2022-03  
Contributor(s):  
Sergii Karpenko  
>fixes https://github.com/Playtika/feign-reactive/issues/434  
>Introduced Retry in ReactiveRetryPolicyfixes https://github.com/Playtika/feign-reactive/issues/418fixes https://github.com/Playtika/feign-reactive/issues/318  
>fixes https://github.com/Playtika/feign-reactive/issues/407  
>Multipart support along with SpringMvcContract  
>spring-cloud-bom:2021.0.1spring-boot-dependencies-bom:2.6.4  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-12  
Contributor(s):  
Sergii Karpenko  
>add CollectionFormat supportfixes https://github.com/Playtika/feign-reactive/issues/412  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-11  
Contributor(s):  
Sergii Karpenko  
>pass empty request parameter in URLfixes https://github.com/Playtika/feign-reactive/issues/413  
>cut tail "/"  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-08  
Contributor(s):  
Karpenko  
>Custom ObjectMapper option introduced  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-07  
Contributor(s):  
Sergii Karpenko  
>BREAKING CHANGE: methodKey includes argument typesfixes https://github.com/Playtika/feign-reactive/issues/395  
>Add support of FormattingConversionServicefixes https://github.com/Playtika/feign-reactive/issues/344  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-05  
Contributor(s):  
Sergii Karpenko  
>Spring Boot 2.4.x / Spring Cloud 3.0.0 https://github.com/Playtika/feign-reactive/issues/329  
>Spring Boot 2.4.x / Spring Cloud 3.0.0https://github.com/Playtika/feign-reactive/issues/329  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-04  
Contributor(s):  
Sergii Karpenko  
>proxy configurationhttps://github.com/Playtika/feign-reactive/issues/315  
>add jetty based web  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-03  
Contributor(s):  
Sergii Karpenko  
>Follow Redirects https://github.com/Playtika/feign-reactive/issues/276  
>Add support for @Param Expanderhttps://github.com/Playtika/feign-reactive/issues/334  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-02  
Contributor(s):  
Sergii Karpenko  
>add test for logs on retry  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2021-01  
Contributor(s):  
Sergii Karpenko  
>cut slash in request url  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-11  
Contributor(s):  
Sergii Karpenko  
Ильиных Илья Сергеевич  
>Indentation changed  
>Reset Request class  
>Hoxton.SR9 & spring-boot 2.3.5.RELEASE  
>Review fixes and additional tests were done.  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-10  
Contributor(s):  
Ильиных Илья Сергеевич  
>Query was added with some workarounds  
>Fixed test with query  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-09  
Contributor(s):  
Tim Koopman  
>Update documentation  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-07  
Contributor(s):  
Taras Danylchuk  
Sergii Karpenko  
>added test that checks if header added by interceptor is get logged  
>introduced two new modules:- bom for easily managing of reactor feign modules- spring cloud starter to enable reactor feign in cloud application with single dependency  
>switch to playtika fork of json-reactor  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-06  
Contributor(s):  
Sergii Karpenko  
>EmptyTarget support added - fixes https://github.com/Playtika/feign-reactive/issues/239  
>pojo as query map - https://github.com/Playtika/feign-reactive/issues/227  
>fix leak on 404 (release body)  
>fix active profile in tests  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-05  
Contributor(s):  
Jorge Diego  
Sergii Karpenko  
>ResponseEntity support added  
>Update README.md (#225)docs: Fix README.md  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-04  
Contributor(s):  
Sergii Karpenko  
Oleksii Bevzenko  
>Feature/circle ci (#210)* Update CircleCI configuration

* Update config.yml  
>#186 IllegalStateException on Duplicate Method Key Parameter  
>fixes https://github.com/Playtika/feign-reactive/issues/209  
>fix CI build  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-03  
Contributor(s):  
Matthew McMahon  
Sergii Karpenko  
>INFRA-57716 Reactive Feign does not work with array request parameters (String[])  
>Attempt to fix deprecations from Feign that been removed with latest Spring Dependencies  
>INFRA-60737 Integrate BlockHound and update test  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-02  
Contributor(s):  
Sergii Karpenko  
>ReactiveHttpExchangeFilterFunction introduced  
>fix url encoding : https://github.com/Playtika/feign-reactive/issues/73  
>Merge pull request #181 from Playtika/feature/test-upsream-contexttest for upstream context added  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2020-01  
Contributor(s):  
Sergii Karpenko  
alekseibevzenko  
Playtika  
dependabot-preview[bot]  
gkomissarov  
>use spring cloud load balancer  
>Feature/test 24 (#177)* Bump spring-boot-dependencies from 2.2.1.RELEASE to 2.2.4.RELEASE

Bumps [spring-boot-dependencies](https://github.com/spring-projects/spring-boot) from 2.2.1.RELEASE to 2.2.4.RELEASE.
- [Release notes](https://github.com/spring-projects/spring-boot/releases)
- [Commits](https://github.com/spring-projects/spring-boot/compare/v2.2.1.RELEASE...v2.2.4.RELEASE)

Signed-off-by: dependabot-preview[bot] <support@dependabot.com>

* Remove jacoco from maven goal

Co-authored-by: dependabot-preview[bot] <27856297+dependabot-preview[bot]@users.noreply.github.com>
Co-authored-by: Oleksii Bevzenko <aleksei.bevzenko@gmail.com>  
>Remove jacoco from maven goal  
>add no codecs availible testcase for webclient  
>Bump awaitility from 3.1.6 to 4.0.2Bumps [awaitility](https://github.com/awaitility/awaitility) from 3.1.6 to 4.0.2.- [Release notes](https://github.com/awaitility/awaitility/releases)- [Changelog](https://github.com/awaitility/awaitility/blob/master/changelog.txt)- [Commits](https://github.com/awaitility/awaitility/compare/awaitility-3.1.6...awaitility-4.0.2)Signed-off-by: dependabot-preview[bot] <support@dependabot.com>  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-12  
Contributor(s):  
Sergii Karpenko  
>fix rx2 flaking test  
>fix rx2 test  
>HOXTON  
>documentation updated: how to pass header in request  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-11  
Contributor(s):  
Sergii Karpenko  
>fix test  
>refactored dependency management section, aligned with spring dependency management  
>last spring jetty sucess  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-05  
Contributor(s):  
skarpenko  
>add test on cloud  
>micrometer  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-04  
Contributor(s):  
skarpenko  
>encode path and query params  
>get rid of commons-httpclient  
>use WebClient.Builder from context in auto-configuration  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-03  
Contributor(s):  
skarpenko  
>set default options only for newly created WebClient  
>added WebClientFeignCustomizer  
>variable in path  
>feign version upgraded to 10.1.0  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-02  
Contributor(s):  
Sergii Karpenko  
skarpenko  
>performance improvement  
>Spring auto-configuration sample project  
>HystrixBadRequestException  
>Update README.md  
>retry on filtered errors test  
>Update README.mdfix https://github.com/Playtika/feign-reactive/issues/37  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2019-01  
Contributor(s):  
aleksei.bevzenko@gmail.com  
Sergii Karpenko  
skarpenko  
>tests on auto-configuration  
>cloud (ribbon/hystrix) auto-configuration  
>cloud feign refactoring  
>composition of request interceptors  
>java 11 HttpClient  
>Extracted logger listener interface so it can be used to collect metrics  
>Change build image  
>Update README.mdrerun codecov  
>increase test coverage  
>Update README.md  
>spring configuration  
>fallback support added without hystrix  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2018-12  
Contributor(s):  
skarpenko  
>benchmark with payload added  
>test added  
>tests coverage  
>test on Spring Mvc annotations as source for reactive feign client  
>upgrade to jetty reactive client 1.0.2 after concurrency bug fixed  
>jetty over http2 transport  
>test on corrupted json added  
>benchmark fixed and stabilized after Spring version updated  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 


2018-11  
Contributor(s):  
skarpenko  
>fix jetty  
>do not overwrite content type header, jetty will do it if header is missed  
>increase tests coverage  
>basic benchmarks  
>tests coverage increased  
>increase test's code coverage  
- - - - - - - - - - - - - - - - - - - - - - - - - - - 

